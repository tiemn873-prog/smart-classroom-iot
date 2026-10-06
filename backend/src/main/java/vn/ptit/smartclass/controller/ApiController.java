package vn.ptit.smartclass.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import vn.ptit.smartclass.dto.Dtos;
import vn.ptit.smartclass.entity.UserProfile;
import vn.ptit.smartclass.repository.UserProfileRepository;
import vn.ptit.smartclass.service.DeviceControlService;
import vn.ptit.smartclass.service.SensorService;

import java.util.List;
import java.time.LocalDateTime;
import org.springframework.format.annotation.DateTimeFormat;

/** Toan bo REST API theo bang o muc 3.4 cua bao cao. */
@RestController
@RequestMapping("/api")
public class ApiController {

    private final UserProfileRepository profileRepository;
    private final SensorService sensorService;
    private final DeviceControlService deviceControlService;

    public ApiController(UserProfileRepository profileRepository,
                         SensorService sensorService,
                         DeviceControlService deviceControlService) {
        this.profileRepository = profileRepository;
        this.sensorService = sensorService;
        this.deviceControlService = deviceControlService;
    }

    /** GET /api/profile - ho so tac gia va cac lien ket tai nguyen */
    @GetMapping("/profile")
    public Dtos.ProfileDto getProfile() {
        UserProfile profile = profileRepository.findAll().stream().findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Chua co ho so"));
        return new Dtos.ProfileDto(profile.getId(), profile.getFullName(), profile.getStudentCode(),
                profile.getBio(), profile.getMajor(), profile.getClassCode(),
                profile.getEmail(), profile.getLocation(),
                profile.getAvatarUrl(), profile.getGithubUrl(),
                profile.getFigmaUrl(), profile.getApiDocsUrl(), profile.getReportPdfUrl());
    }

    /** GET /api/devices - danh sach thiet bi kem trang thai hien tai */
    @GetMapping("/devices")
    public List<Dtos.DeviceDto> getDevices() {
        return deviceControlService.listDevices();
    }

    /** POST /api/devices/control/{id} - gui lenh bat/tat, cho ESP32 bao ket qua */
    @PostMapping("/devices/control/{id}")
    public Dtos.ControlResponse control(@PathVariable Long id, @RequestBody Dtos.ControlRequest request) {
        if (request == null || request.command() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Thieu truong command");
        }
        return deviceControlService.controlDevice(id, request.command().trim().toUpperCase());
    }

    /** POST /api/devices/control-all - body {"action":"ON"} hoac {"action":"OFF"}. */
    @PostMapping("/devices/control-all")
    public ResponseEntity<Dtos.ControlAllResponse> controlAll(@RequestBody Dtos.ControlAllRequest request) {
        if (request == null || request.action() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Thieu truong action");
        }
        List<Dtos.ControlResponse> results = deviceControlService.controlAll(
                request.action().trim().toUpperCase(java.util.Locale.ROOT));
        boolean success = results.stream().allMatch(result -> "SUCCESS".equals(result.status()));
        int code = success ? 200 : 503;
        String message = success ? "Dieu khien toan bo thiet bi thanh cong"
                : "Co thiet bi khong phan hoi trong thoi gian cho";
        return ResponseEntity.status(code).body(new Dtos.ControlAllResponse(success, code, message, results));
    }

    /** GET /api/sensors/latest - so do moi nhat + 12 diem cho bieu do */
    @GetMapping("/sensors/latest")
    public Dtos.LatestSensorDto getLatestSensors() {
        return sensorService.getLatest();
    }

    /** GET /api/sensors - lich su do dac, co loc va phan trang */
    @GetMapping("/sensors")
    public Dtos.PageResponse<Dtos.MetricDto> getSensors(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime measuredAt,
            @RequestParam(required = false) Double value,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false, defaultValue = "ALL") String sensorType,
            @RequestParam(required = false, defaultValue = "DESC") String sort,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        return sensorService.search(keyword, sensorType, sort, page, Math.min(Math.max(size, 1), 200), measuredAt, value);
    }

    /** GET /api/action-history - nhat ky dieu khien thiet bi */
    @GetMapping("/action-history")
    public Dtos.PageResponse<Dtos.ControlLogDto> getActionHistory(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime operatedAt,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long deviceId,
            @RequestParam(required = false, defaultValue = "ALL") String command,
            @RequestParam(required = false, defaultValue = "ALL") String status,
            @RequestParam(required = false, defaultValue = "DESC") String sort,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        return deviceControlService.searchHistory(keyword, deviceId, command, status, sort, page, Math.min(Math.max(size, 1), 200), operatedAt);
    }
}
