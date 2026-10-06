package vn.ptit.smartclass.dto;

import java.time.LocalDateTime;
import java.util.List;

/** Cac ban ghi du lieu tra ve cho Frontend. */
public final class Dtos {

    private Dtos() {
    }

    /** GET /api/profile */
    public record ProfileDto(Long id, String fullName, String studentCode, String bio,
                             String major, String classCode, String email, String location,
                             String avatarUrl, String githubUrl, String figmaUrl,
                             String apiDocsUrl, String reportPdfUrl) {
    }

    /** GET /api/devices */
    public record DeviceDto(Long id, String deviceCode, String name, String type,
                            String currentState, LocalDateTime lastSynced) {
    }

    /** Mot ban ghi trong bang Du lieu cam bien */
    public record MetricDto(Long id, String type, String unit, Double value, LocalDateTime timestamp) {
    }

    /** Mot diem tren bieu do Dashboard - gop 3 loai cam bien cung moc thoi gian */
    public record ChartPointDto(LocalDateTime time, Double temperature, Double humidity, Double light) {
    }

    /** GET /api/sensors/latest */
    public record LatestSensorDto(Double temperature, Double humidity, Double light,
                                  LocalDateTime timestamp, boolean hardwareOnline,
                                  List<ChartPointDto> history) {
    }

    /** Mot dong trong bang Lich su hoat dong */
    public record ControlLogDto(Long id, Long deviceId, String deviceName, String command,
                                String status, LocalDateTime requestedAt, LocalDateTime completedAt) {
    }

    /** Khung phan trang dung chung cho /api/sensors va /api/action-history */
    public record PageResponse<T>(List<T> items, int page, int size, long total, int totalPages) {
    }

    /** Body cua POST /api/devices/control/{id} */
    public record ControlRequest(String command) {
    }

    /** Ket qua dieu khien thiet bi */
    public record ControlResponse(Long logId, Long deviceId, String deviceCode, String state, String status) {
    }

    /** Ban tin day xuong /topic/status */
    public record HardwareStatusDto(String deviceCode, String status) {
    }
}
