package vn.ptit.smartclass.service;

import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Predicate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import vn.ptit.smartclass.dto.Dtos;
import vn.ptit.smartclass.entity.ControlLog;
import vn.ptit.smartclass.entity.SmartDevice;
import vn.ptit.smartclass.mqtt.MqttPayloads;
import vn.ptit.smartclass.mqtt.MqttService;
import vn.ptit.smartclass.repository.ControlLogRepository;
import vn.ptit.smartclass.repository.SmartDeviceRepository;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Dieu khien thiet bi theo co che MOT PHA.
 *
 *   1. Ghi control_logs = PENDING, lay id lam requestId
 *   2. Publish device/control/{id}
 *   3. ESP32 dong/cat thiet bi ngay roi tra device/status
 *   4. Backend cap nhat smart_devices + control_logs = SUCCESS, tra 200 OK
 *   5. Qua han khong thay device/status -> control_logs = TIMEOUT, tra 503
 *      (chi xay ra khi ESP32 mat dien hoac rot mang)
 */
@Service
public class DeviceControlService {

    private static final Logger log = LoggerFactory.getLogger(DeviceControlService.class);

    private static final String STATUS_PENDING = "PENDING";
    private static final String STATUS_SUCCESS = "SUCCESS";
    private static final String STATUS_TIMEOUT = "TIMEOUT";

    private final SmartDeviceRepository deviceRepository;
    private final ControlLogRepository logRepository;
    private final MqttService mqtt;
    private final RealtimePublisher realtime;
    private final HardwareStatusHolder hardwareStatus;
    private final long timeoutMs;

    /** Cac lenh dang cho ESP32 bao ket qua, khoa theo id cua control_logs */
    private final Map<Long, CompletableFuture<SmartDevice>> pendingRequests = new ConcurrentHashMap<>();

    public DeviceControlService(SmartDeviceRepository deviceRepository,
                                ControlLogRepository logRepository,
                                MqttService mqtt,
                                RealtimePublisher realtime,
                                HardwareStatusHolder hardwareStatus,
                                @Value("${app.control.timeout-ms}") long timeoutMs) {
        this.deviceRepository = deviceRepository;
        this.logRepository = logRepository;
        this.mqtt = mqtt;
        this.realtime = realtime;
        this.hardwareStatus = hardwareStatus;
        this.timeoutMs = timeoutMs;
    }

    @Transactional(readOnly = true)
    public List<Dtos.DeviceDto> listDevices() {
        return deviceRepository.findAllByOrderByIdAsc().stream().map(this::toDto).toList();
    }

    /**
     * POST /api/devices/control/{id}
     * Co y de phuong thuc nay CHO (block) den khi ESP32 bao xong hoac het han,
     * nho vay web chi doi trang thai khi thiet bi that su da doi.
     */
    public Dtos.ControlResponse controlDevice(Long deviceId, String command) {
        if (!"TURN_ON".equals(command) && !"TURN_OFF".equals(command)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "command chi nhan TURN_ON hoac TURN_OFF");
        }

        SmartDevice device = deviceRepository.findById(deviceId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Khong co thiet bi id = " + deviceId));

        // Ghi nhat ky PENDING truoc khi gui lenh, id cua no chinh la requestId
        ControlLog controlLog = new ControlLog();
        controlLog.setDevice(device);
        controlLog.setActionCommand(command);
        controlLog.setSyncStatus(STATUS_PENDING);
        controlLog.setRequestedAt(LocalDateTime.now());
        controlLog = logRepository.save(controlLog);
        Long requestId = controlLog.getId();

        CompletableFuture<SmartDevice> future = new CompletableFuture<>();
        pendingRequests.put(requestId, future);

        // Gui lenh xuong ESP32 - ESP32 thuc thi ngay khi nhan duoc
        mqtt.publish("device/control/" + deviceId,
                "{\"command\":\"" + command + "\",\"requestId\":\"" + requestId + "\"}");

        try {
            // Cho ESP32 thuc thi xong va bao trang thai that
            SmartDevice updated = future.get(timeoutMs, TimeUnit.MILLISECONDS);
            return new Dtos.ControlResponse(requestId, updated.getId(), updated.getDeviceCode(),
                    updated.getCurrentState(), STATUS_SUCCESS);

        } catch (TimeoutException e) {
            // Qua han -> ghi TIMEOUT, giu nguyen current_state vi khong biet thuc te ra sao
            markTimeout(requestId);
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "Thiet bi khong phan hoi trong " + timeoutMs + "ms");

        } catch (Exception e) {
            markTimeout(requestId);
            Thread.currentThread().interrupt();
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "Loi khi cho thiet bi phan hoi: " + e.getMessage());

        } finally {
            pendingRequests.remove(requestId);
        }
    }

    /** Khong danh dau @Transactional: phuong thuc nay duoc goi tu chinh lop nay,
     *  proxy cua Spring se khong chay qua. Moi loi goi repository tu mo giao dich rieng. */
    private void markTimeout(Long requestId) {
        logRepository.findById(requestId).ifPresent(entry -> {
            if (STATUS_PENDING.equals(entry.getSyncStatus())) {
                entry.setSyncStatus(STATUS_TIMEOUT);
                logRepository.save(entry);
            }
        });
        log.warn("[CONTROL] requestId={} qua han, ghi TIMEOUT", requestId);
    }

    /** ESP32 bao trang thai thuc te -> cap nhat DB va bao len Web */
    @Transactional
    public void handleStatus(MqttPayloads.StatusPayload status) {
        if (status.deviceId() == null || status.state() == null) {
            return;
        }

        SmartDevice device = deviceRepository.findById(status.deviceId()).orElse(null);
        if (device == null) {
            log.warn("[CONTROL] ESP32 bao trang thai cho thiet bi la: id={}", status.deviceId());
            return;
        }

        device.setCurrentState(status.state());
        device.setLastSynced(LocalDateTime.now());
        deviceRepository.save(device);

        Long requestId = parseRequestId(status.requestId());
        if (requestId != null) {
            logRepository.findById(requestId).ifPresent(entry -> {
                if (STATUS_PENDING.equals(entry.getSyncStatus())) {
                    entry.setSyncStatus(STATUS_SUCCESS);
                    entry.setCompletedAt(LocalDateTime.now());
                    logRepository.save(entry);
                }
            });

            CompletableFuture<SmartDevice> waiting = pendingRequests.remove(requestId);
            if (waiting != null) {
                waiting.complete(device);
            }
        }

        hardwareStatus.setOnline(true);
        realtime.sendDeviceUpdate(toDto(device));
        log.info("[CONTROL] {} -> {} (nguon: {})", device.getDeviceCode(), status.state(), status.source());
    }

    /** Co che LWT: broker bao ESP32 song hay chet */
    public void handleHardwareStatus(MqttPayloads.HardwarePayload payload) {
        boolean online = "ONLINE".equalsIgnoreCase(payload.status());
        hardwareStatus.setOnline(online);
        realtime.sendHardwareStatus(new Dtos.HardwareStatusDto(payload.deviceCode(), payload.status()));
        log.info("[HARDWARE] {} {}", payload.deviceCode(), payload.status());
    }

    /** GET /api/action-history - nhat ky dieu khien co loc va phan trang */
    @Transactional(readOnly = true)
    public Dtos.PageResponse<Dtos.ControlLogDto> searchHistory(String keyword, Long deviceId,
                                                               String command, String status,
                                                               String sort, int page, int size, LocalDateTime operatedAt) {
        Sort.Direction direction = "ASC".equalsIgnoreCase(sort) ? Sort.Direction.ASC : Sort.Direction.DESC;
        Pageable pageable = PageRequest.of(Math.max(0, page - 1), size, Sort.by(direction, "requestedAt", "id"));

        Specification<ControlLog> spec = (root, query, cb) -> {
            List<Predicate> conditions = new ArrayList<>();
            if (operatedAt != null) {
                LocalDateTime start = operatedAt.withSecond(0).withNano(0);
                conditions.add(cb.greaterThanOrEqualTo(root.<LocalDateTime>get("requestedAt"), start));
                conditions.add(cb.lessThan(root.<LocalDateTime>get("requestedAt"), start.plusMinutes(1)));
            }


            if (deviceId != null) {
                conditions.add(cb.equal(root.get("device").get("id"), deviceId));
            }
            if (command != null && !command.isBlank() && !"ALL".equalsIgnoreCase(command)) {
                conditions.add(cb.equal(root.get("actionCommand"), command));
            }
            // Loc theo ket qua dong bo: PENDING, SUCCESS hoac TIMEOUT
            if (status != null && !status.isBlank() && !"ALL".equalsIgnoreCase(status)) {
                conditions.add(cb.equal(root.get("syncStatus"), status));
            }
            if (keyword != null && !keyword.isBlank()) {
                String term = "%" + keyword.trim().replace("#", "") + "%";

                /* Doi thoi gian sang dung chuoi hien tren man hinh roi moi so khop,
                   de nguoi dung go y nhu nhin thay la ra ket qua. */
                Expression<String> thoiGianHienThi = cb.function(
                        "date_format", String.class,
                        root.get("requestedAt"), cb.literal("%d-%m-%Y %H:%i:%s"));

                conditions.add(cb.or(
                        cb.like(root.get("id").as(String.class), term),
                        cb.like(thoiGianHienThi, term)));
            }
            return cb.and(conditions.toArray(new Predicate[0]));
        };

        Page<ControlLog> result = logRepository.findAll(spec, pageable);
        List<Dtos.ControlLogDto> items = result.getContent().stream()
                .map(entry -> new Dtos.ControlLogDto(
                        entry.getId(),
                        entry.getDevice().getId(),
                        entry.getDevice().getName(),
                        entry.getActionCommand(),
                        entry.getSyncStatus(),
                        entry.getRequestedAt(),
                        entry.getCompletedAt()))
                .toList();

        return new Dtos.PageResponse<>(items, page, size, result.getTotalElements(), result.getTotalPages());
    }

    private Long parseRequestId(String raw) {
        try {
            return raw == null || raw.isBlank() ? null : Long.parseLong(raw.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private Dtos.DeviceDto toDto(SmartDevice device) {
        return new Dtos.DeviceDto(device.getId(), device.getDeviceCode(), device.getName(),
                device.getType(), device.getCurrentState(), device.getLastSynced());
    }
}
