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
import java.util.concurrent.Executors;

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

    // Logger ghi thông tin xử lý ra console; không phải dữ liệu nhật ký trong MySQL.
    private static final Logger log = LoggerFactory.getLogger(DeviceControlService.class);

    private static final String STATUS_PENDING = "PENDING";
    private static final String STATUS_SUCCESS = "SUCCESS";
    private static final String STATUS_TIMEOUT = "TIMEOUT";

    // Repository đọc/ghi MySQL; mqtt gửi lệnh; realtime đẩy thay đổi xuống trình duyệt.
    private final SmartDeviceRepository deviceRepository;
    private final ControlLogRepository logRepository;
    private final MqttService mqtt;
    private final RealtimePublisher realtime;
    private final HardwareStatusHolder hardwareStatus;
    private final long timeoutMs;

    /** Cac lenh dang cho ESP32 bao ket qua, khoa theo id cua control_logs */
    // CompletableFuture là một kết quả sẽ có sau: request HTTP chờ, luồng nhận MQTT điền kết quả.
    // ConcurrentHashMap cho phép các luồng HTTP và MQTT cùng truy cập danh sách chờ.
    private final Map<Long, CompletableFuture<SmartDevice>> pendingRequests = new ConcurrentHashMap<>();

    // Spring tự truyền các bean vào constructor; @Value đọc timeout từ application.properties.
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
        // stream() duyệt danh sách; map(this::toDto) đổi mỗi entity thành DTO; toList() gom kết quả.
        return deviceRepository.findAllByOrderByIdAsc().stream().map(this::toDto).toList();
    }

    /**
     * POST /api/devices/control/{id}
     * Co y de phuong thuc nay CHO (block) den khi ESP32 bao xong hoac het han,
     * nho vay web chi doi trang thai khi thiet bi that su da doi.
     */
    public Dtos.ControlResponse controlDevice(Long deviceId, String command) {
        Dtos.ControlResponse result = executeControl(deviceId, command);
        // API điều khiển riêng trả HTTP 503 nếu không nhận được kết quả thành công.
        if (!STATUS_SUCCESS.equals(result.status())) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "Thiet bi khong phan hoi trong " + timeoutMs + "ms");
        }
        return result;
    }

    /** Mot REST request, gui lenh song song va thu ket qua rieng tung thiet bi. */
    public List<Dtos.ControlResponse> controlAll(String action) {
        // Frontend gửi ON/OFF, còn giao thức điều khiển ESP32 dùng TURN_ON/TURN_OFF.
        String command = switch (action) {
            case "ON" -> "TURN_ON";
            case "OFF" -> "TURN_OFF";
            default -> throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "action chi nhan ON hoac OFF");
        };
        List<SmartDevice> devices = deviceRepository.findAllByOrderByIdAsc();
        // Mỗi thiết bị chạy trên một virtual thread để thời gian chờ không cộng dồn từng thiết bị.
        // try (...) tự đóng executor sau khi xử lý xong.
        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            List<CompletableFuture<Dtos.ControlResponse>> tasks = devices.stream()
                    .map(device -> CompletableFuture.supplyAsync(
                            () -> executeControl(device.getId(), command), executor))
                    .toList();
            // Các tác vụ đã được khởi chạy; join() chờ và lấy kết quả của từng tác vụ.
            return tasks.stream().map(CompletableFuture::join).toList();
        }
    }

    private Dtos.ControlResponse executeControl(Long deviceId, String command) {
        // Chặn lệnh ngoài hai giá trị được hỗ trợ; BAD_REQUEST tương ứng HTTP 400.
        if (!"TURN_ON".equals(command) && !"TURN_OFF".equals(command)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "command chi nhan TURN_ON hoac TURN_OFF");
        }

        // findById() trả Optional; orElseThrow() trả lỗi 404 nếu không có thiết bị.
        SmartDevice device = deviceRepository.findById(deviceId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Khong co thiet bi id = " + deviceId));

        // Ghi nhat ky PENDING truoc khi gui lenh, id cua no chinh la requestId
        ControlLog controlLog = new ControlLog();
        controlLog.setDevice(device);
        controlLog.setActionCommand(command);
        controlLog.setSyncStatus(STATUS_PENDING);
        controlLog.setRequestedAt(LocalDateTime.now());
        // save() lưu bản ghi và lấy ID tự tăng do MySQL cấp.
        controlLog = logRepository.save(controlLog);
        Long requestId = controlLog.getId();

        // Đăng ký chờ trước khi gửi MQTT để có thể nhận cả phản hồi đến rất nhanh.
        CompletableFuture<SmartDevice> future = new CompletableFuture<>();
        pendingRequests.put(requestId, future);

        // Gui lenh xuong ESP32 - ESP32 thuc thi ngay khi nhan duoc
        // Ví dụ topic device/control/1, JSON {"command":"TURN_ON","requestId":"15"}.
        // requestId nối phản hồi ESP32 với đúng bản ghi control_logs và future đang chờ.
        mqtt.publish("device/control/" + deviceId,
                "{\"command\":\"" + command + "\",\"requestId\":\"" + requestId + "\"}");

        try {
            // Cho ESP32 thuc thi xong va bao trang thai that
            // get() dừng luồng này đến khi handleStatus() gọi complete(), hoặc hết timeout.
            SmartDevice updated = future.get(timeoutMs, TimeUnit.MILLISECONDS);
            return new Dtos.ControlResponse(requestId, updated.getId(), updated.getDeviceCode(),
                    updated.getCurrentState(), STATUS_SUCCESS);

        } catch (TimeoutException e) {
            // Qua han -> ghi TIMEOUT, giu nguyen current_state vi khong biet thuc te ra sao
            markTimeout(requestId);
            return new Dtos.ControlResponse(requestId, deviceId, device.getDeviceCode(),
                    device.getCurrentState(), STATUS_TIMEOUT);

        } catch (Exception e) {
            markTimeout(requestId);
            // Khôi phục cờ ngắt để phần quản lý luồng biết tác vụ đã bị yêu cầu dừng.
            if (e instanceof InterruptedException) Thread.currentThread().interrupt();
            return new Dtos.ControlResponse(requestId, deviceId, device.getDeviceCode(),
                    device.getCurrentState(), STATUS_TIMEOUT);

        } finally {
            // Luôn dọn danh sách chờ, dù thành công, hết hạn hay gặp lỗi.
            pendingRequests.remove(requestId);
        }
    }

    /** Khong danh dau @Transactional: phuong thuc nay duoc goi tu chinh lop nay,
     *  proxy cua Spring se khong chay qua. Moi loi goi repository tu mo giao dich rieng. */
    private void markTimeout(Long requestId) {
        logRepository.findById(requestId).ifPresent(entry -> {
            // Chỉ đổi lệnh còn chờ; không ghi đè một nhật ký đã thành công.
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
        // Transaction bao quanh thao tác DB của phương thức khi được gọi qua bean Spring.
        // Bỏ qua bản tin thiếu ID hoặc trạng thái vì không đủ thông tin để cập nhật.
        if (status.deviceId() == null || status.state() == null) {
            return;
        }

        SmartDevice device = deviceRepository.findById(status.deviceId()).orElse(null);
        if (device == null) {
            log.warn("[CONTROL] ESP32 bao trang thai cho thiet bi la: id={}", status.deviceId());
            return;
        }

        // Lưu trạng thái ESP32 báo về, thay vì tự đoán thiết bị đã đổi ngay khi gửi lệnh.
        device.setCurrentState(status.state());
        device.setLastSynced(LocalDateTime.now());
        deviceRepository.save(device);

        // Bản tin không có requestId vẫn cập nhật thiết bị, nhưng không gắn với lệnh đang chờ.
        Long requestId = parseRequestId(status.requestId());
        if (requestId != null) {
            logRepository.findById(requestId).ifPresent(entry -> {
                if (STATUS_PENDING.equals(entry.getSyncStatus())) {
                    entry.setSyncStatus(STATUS_SUCCESS);
                    entry.setCompletedAt(LocalDateTime.now());
                    logRepository.save(entry);
                }
            });

            // remove() vừa lấy future vừa xóa khỏi map; phản hồi muộn có thể không còn future.
            CompletableFuture<SmartDevice> waiting = pendingRequests.remove(requestId);
            if (waiting != null) {
                // Đưa thiết bị đã cập nhật vào future để future.get() bên executeControl() tiếp tục.
                waiting.complete(device);
            }
        }

        hardwareStatus.setOnline(true);
        // Gửi đến /topic/device để các trình duyệt đang mở đều nhận được trạng thái mới.
        realtime.sendDeviceUpdate(toDto(device));
        log.info("[CONTROL] {} -> {} (nguon: {})", device.getDeviceCode(), status.state(), status.source());
    }

    /** Co che LWT: broker bao ESP32 song hay chet */
    public void handleHardwareStatus(MqttPayloads.HardwarePayload payload) {
        // equalsIgnoreCase() so sánh không phân biệt hoa/thường; chỉ ONLINE được xem là online.
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
        // ASC: cũ trước; còn lại: mới trước. ID dùng để sắp xếp thêm khi thời gian trùng nhau.
        Sort.Direction direction = "ASC".equalsIgnoreCase(sort) ? Sort.Direction.ASC : Sort.Direction.DESC;
        // Frontend đếm trang từ 1, Spring đếm từ 0; Math.max() chặn chỉ số trang âm.
        Pageable pageable = PageRequest.of(Math.max(0, page - 1), size, Sort.by(direction, "requestedAt", "id"));

        // Specification tạo WHERE động: chỉ thêm điều kiện cho bộ lọc người dùng có chọn.
        // root là entity ControlLog, cb là công cụ tạo biểu thức SQL, Predicate là điều kiện.
        Specification<ControlLog> spec = (root, query, cb) -> {
            List<Predicate> conditions = new ArrayList<>();
            if (operatedAt != null) {
                // Lọc cả phút đã chọn: >= đầu phút và < đầu phút kế tiếp, không đòi khớp giây.
                LocalDateTime start = operatedAt.withSecond(0).withNano(0);
                conditions.add(cb.greaterThanOrEqualTo(root.<LocalDateTime>get("requestedAt"), start));
                conditions.add(cb.lessThan(root.<LocalDateTime>get("requestedAt"), start.plusMinutes(1)));
            }


            if (deviceId != null) {
                // Đi từ nhật ký sang thiết bị liên kết rồi so sánh ID thiết bị.
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
                // %...% là tìm chuỗi có chứa từ khóa trong SQL LIKE; bỏ # người dùng nhập.
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
            // AND: bản ghi phải thỏa tất cả bộ lọc; nhóm từ khóa phía trên dùng OR.
            return cb.and(conditions.toArray(new Predicate[0]));
        };

        // Truy vấn theo WHERE và phân trang; Page chứa dữ liệu trang hiện tại cùng tổng kết quả.
        Page<ControlLog> result = logRepository.findAll(spec, pageable);
        // Chỉ đưa các trường frontend cần vào DTO, gồm tên thiết bị từ quan hệ entity.
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

        // total là tổng bản ghi phù hợp ở mọi trang; items chỉ gồm bản ghi của trang hiện tại.
        return new Dtos.PageResponse<>(items, page, size, result.getTotalElements(), result.getTotalPages());
    }

    private Long parseRequestId(String raw) {
        // Chuyển chuỗi MQTT sang ID số; thiếu hoặc sai định dạng thì trả null để bỏ qua việc ghép lệnh.
        try {
            return raw == null || raw.isBlank() ? null : Long.parseLong(raw.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private Dtos.DeviceDto toDto(SmartDevice device) {
        // Tách cấu trúc trả ra frontend khỏi entity dùng để lưu trong MySQL.
        return new Dtos.DeviceDto(device.getId(), device.getDeviceCode(), device.getName(),
                device.getType(), device.getCurrentState(), device.getLastSynced());
    }
}
