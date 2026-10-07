package vn.ptit.smartclass.service;

import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Predicate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.ptit.smartclass.dto.Dtos;
import vn.ptit.smartclass.entity.ClassSensor;
import vn.ptit.smartclass.entity.EnvironmentalMetric;
import vn.ptit.smartclass.mqtt.MqttPayloads;
import vn.ptit.smartclass.repository.ClassSensorRepository;
import vn.ptit.smartclass.repository.EnvironmentalMetricRepository;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Xu ly so lieu cam bien: nhan tu MQTT -> luu MySQL -> day xuong Web,
 * va phuc vu cac API tra cuu lich su.
 */
@Service
public class SensorService {

    private static final Logger log = LoggerFactory.getLogger(SensorService.class);

    // Các khóa này phải khớp category trong class_sensors và loại cảm biến ở frontend.
    public static final String TEMPERATURE = "temperature";
    public static final String HUMIDITY = "humidity";
    public static final String LIGHT = "light";

    /** So diem hien tren bieu do Dashboard */
    private static final int CHART_POINTS = 12;

    // sensorRepository quản lý danh mục cảm biến; metricRepository quản lý từng lần đo.
    // realtime gửi WebSocket; hardwareStatus giữ cờ online/offline trong bộ nhớ.
    private final ClassSensorRepository sensorRepository;
    private final EnvironmentalMetricRepository metricRepository;
    private final RealtimePublisher realtime;
    private final HardwareStatusHolder hardwareStatus;

    // Spring tự truyền các bean phụ thuộc vào constructor khi tạo service.
    public SensorService(ClassSensorRepository sensorRepository,
                         EnvironmentalMetricRepository metricRepository,
                         RealtimePublisher realtime,
                         HardwareStatusHolder hardwareStatus) {
        this.sensorRepository = sensorRepository;
        this.metricRepository = metricRepository;
        this.realtime = realtime;
        this.hardwareStatus = hardwareStatus;
    }

    /**
     * Buoc 6-9 Hinh 3: mot ban tin data/sensor sinh ra 3 ban ghi environmental_metrics
     * (nhiet do, do am, anh sang) roi day ngay xuong Frontend.
     */
    @Transactional
    public void handleSensorReading(MqttPayloads.SensorPayload payload) {
        // Các thao tác lưu DB trong phương thức cùng một transaction khi gọi qua bean Spring.
        // Dùng thời gian backend nhận bản tin, chung cho cả ba số đo; không dùng uptime của ESP32.
        LocalDateTime now = LocalDateTime.now();

        // Một bản tin đủ ba giá trị tạo ba hàng environmental_metrics, mỗi hàng thuộc một cảm biến.
        saveMetric(TEMPERATURE, payload.temperature(), now);
        saveMetric(HUMIDITY, payload.humidity(), now);
        saveMetric(LIGHT, payload.light(), now);

        // ESP32 con gui du lieu tuc la phan cung dang song
        hardwareStatus.setOnline(true);

        // Gộp ba giá trị thành một điểm rồi gửi /topic/sensor để Dashboard cập nhật ngay.
        realtime.sendSensorUpdate(new Dtos.ChartPointDto(
                now, payload.temperature(), payload.humidity(), payload.light()));

        log.debug("[SENSOR] T={} H={} L={}", payload.temperature(), payload.humidity(), payload.light());
    }

    private void saveMetric(String category, Double value, LocalDateTime time) {
        // Double cho phép null: thiếu giá trị thì bỏ qua, không lưu thành số 0 giả.
        if (value == null) {
            return;
        }
        // Tìm cảm biến đã khai báo; orElse(null) trả null nếu danh mục chưa có loại này.
        ClassSensor sensor = sensorRepository.findByCategory(category).orElse(null);
        if (sensor == null) {
            log.warn("[SENSOR] Chua khai bao cam bien loai '{}' trong bang class_sensors", category);
            return;
        }
        // Liên kết số đo với cảm biến qua sensor_id, rồi lưu giá trị và thời gian đo.
        EnvironmentalMetric metric = new EnvironmentalMetric();
        metric.setSensor(sensor);
        metric.setReadingValue(value);
        metric.setTimestamp(time);
        metricRepository.save(metric);
    }

    /** GET /api/sensors/latest - so hien tai + 12 diem gan nhat cho bieu do */
    @Transactional(readOnly = true)
    public Dtos.LatestSensorDto getLatest() {
        // readOnly cho biết phương thức chỉ đọc DB; lấy giá trị mới nhất riêng từng loại.
        Double temperature = latestValue(TEMPERATURE);
        Double humidity = latestValue(HUMIDITY);
        Double light = latestValue(LIGHT);

        // Thời gian tổng hợp lấy từ bản ghi nhiệt độ mới nhất; chưa có thì là null.
        LocalDateTime timestamp = metricRepository
                .findFirstBySensor_CategoryOrderByTimestampDesc(TEMPERATURE)
                .map(EnvironmentalMetric::getTimestamp)
                .orElse(null);

        // Một response phục vụ cả ba thẻ số liệu, trạng thái phần cứng và biểu đồ khi mở trang.
        return new Dtos.LatestSensorDto(temperature, humidity, light, timestamp,
                hardwareStatus.isOnline(), buildHistory());
    }

    private Double latestValue(String category) {
        // Repository trả Optional: map() lấy giá trị nếu có bản ghi; orElse(null) khi chưa có dữ liệu.
        return metricRepository.findFirstBySensor_CategoryOrderByTimestampDesc(category)
                .map(EnvironmentalMetric::getReadingValue)
                .orElse(null);
    }

    /** Ghep 3 chuoi do thanh cac diem theo thoi gian, sap xep cu -> moi */
    private List<Dtos.ChartPointDto> buildHistory() {
        // Trang số 0 là trang đầu của Spring; giới hạn mỗi loại tối đa CHART_POINTS bản ghi.
        Pageable limit = PageRequest.of(0, CHART_POINTS);
        List<EnvironmentalMetric> temps = metricRepository
                .findBySensor_CategoryOrderByTimestampDesc(TEMPERATURE, limit);
        List<EnvironmentalMetric> hums = metricRepository
                .findBySensor_CategoryOrderByTimestampDesc(HUMIDITY, limit);
        List<EnvironmentalMetric> lights = metricRepository
                .findBySensor_CategoryOrderByTimestampDesc(LIGHT, limit);

        // Lấy độ dài nhỏ nhất để không truy cập vượt danh sách nếu một loại thiếu dữ liệu.
        int size = Math.min(temps.size(), Math.min(hums.size(), lights.size()));
        List<Dtos.ChartPointDto> history = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            // Ghép theo vị trí i của ba danh sách mới nhất, dùng thời gian của nhiệt độ.
            // Đây không phải phép ghép theo timestamp: nếu thiếu số đo, các mốc có thể lệch nhau.
            history.add(new Dtos.ChartPointDto(
                    temps.get(i).getTimestamp(),
                    temps.get(i).getReadingValue(),
                    hums.get(i).getReadingValue(),
                    lights.get(i).getReadingValue()));
        }
        // DB trả mới → cũ; đảo lại cũ → mới để biểu đồ chạy thời gian từ trái sang phải.
        Collections.reverse(history);
        return history;
    }

    /** GET /api/sensors - tra cuu lich su co loc, sap xep, phan trang */
    @Transactional(readOnly = true)
    public Dtos.PageResponse<Dtos.MetricDto> search(String keyword, String sensorType,
                                                    String sort, int page, int size, LocalDateTime measuredAt, Double value) {
        // ASC là cũ trước, DESC là mới trước; ID phân định thứ tự khi timestamp trùng nhau.
        Sort.Direction direction = "ASC".equalsIgnoreCase(sort) ? Sort.Direction.ASC : Sort.Direction.DESC;
        // Đổi trang frontend (bắt đầu từ 1) sang trang Spring (bắt đầu từ 0), chặn chỉ số âm.
        Pageable pageable = PageRequest.of(Math.max(0, page - 1), size, Sort.by(direction, "timestamp", "id"));

        // Specification xây WHERE tùy bộ lọc: root là EnvironmentalMetric, cb tạo biểu thức SQL.
        // Mỗi Predicate trong conditions là một điều kiện phải thỏa.
        Specification<EnvironmentalMetric> spec = (root, query, cb) -> {
            List<Predicate> conditions = new ArrayList<>();
            if (measuredAt != null) {
                // Tìm mọi số đo trong phút đã chọn: >= đầu phút và < đầu phút tiếp theo.
                LocalDateTime start = measuredAt.withSecond(0).withNano(0);
                conditions.add(cb.greaterThanOrEqualTo(root.<LocalDateTime>get("timestamp"), start));
                conditions.add(cb.lessThan(root.<LocalDateTime>get("timestamp"), start.plusMinutes(1)));
            }
            if (value != null) {
                // So sánh đúng giá trị được nhập, không phải tìm gần đúng hay theo khoảng.
                conditions.add(cb.equal(root.get("readingValue"), value));
            }


            if (sensorType != null && !sensorType.isBlank() && !"ALL".equalsIgnoreCase(sensorType)) {
                // Đi qua quan hệ sensor để lọc category; ALL nghĩa là không thêm điều kiện loại.
                conditions.add(cb.equal(root.get("sensor").get("category"), sensorType));
            }
            if (keyword != null && !keyword.isBlank()) {
                // %...% dùng SQL LIKE để tìm chuỗi chứa từ khóa; trim() bỏ khoảng trắng hai đầu.
                String term = "%" + keyword.trim().replace("#", "") + "%";

                /* Doi timestamp sang dung chuoi ma nguoi dung nhin thay tren man
                   hinh (25-09-2026 21:33:41) roi moi so khop. Neu so khop thang
                   voi kieu luu trong CSDL (2026-09-25...) thi go y nhu tren man
                   hinh lai khong ra ket qua nao. */
                Expression<String> thoiGianHienThi = cb.function(
                        "date_format", String.class,
                        root.get("timestamp"), cb.literal("%d-%m-%Y %H:%i:%s"));

                // OR: từ khóa khớp thời gian hiển thị hoặc giá trị đo đã chuyển thành chuỗi.
                conditions.add(cb.or(
                        cb.like(thoiGianHienThi, term),
                        cb.like(root.get("readingValue").as(String.class), term)));
            }
            // AND kết hợp tất cả bộ lọc; không có bộ lọc thì không giới hạn bằng các điều kiện này.
            return cb.and(conditions.toArray(new Predicate[0]));
        };

        // Chạy truy vấn có lọc, sắp xếp và phân trang; Page còn chứa tổng bản ghi/tổng trang.
        Page<EnvironmentalMetric> result = metricRepository.findAll(spec, pageable);
        // stream().map() đổi từng entity của trang hiện tại thành DTO frontend cần.
        List<Dtos.MetricDto> items = result.getContent().stream()
                .map(m -> new Dtos.MetricDto(
                        m.getId(),
                        m.getSensor().getCategory(),
                        m.getSensor().getUnitLabel(),
                        m.getReadingValue(),
                        m.getTimestamp()))
                .toList();

        // items chỉ là trang hiện tại; total là số bản ghi phù hợp của tất cả các trang.
        return new Dtos.PageResponse<>(items, page, size, result.getTotalElements(), result.getTotalPages());
    }
}
