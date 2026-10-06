package vn.ptit.smartclass.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.*;
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

    public static final String TEMPERATURE = "temperature";
    public static final String HUMIDITY = "humidity";
    public static final String LIGHT = "light";

    /** So diem hien tren bieu do Dashboard */
    private static final int CHART_POINTS = 12;

    private final ClassSensorRepository sensorRepository;
    private final EnvironmentalMetricRepository metricRepository;
    private final RealtimePublisher realtime;
    private final HardwareStatusHolder hardwareStatus;

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
        LocalDateTime now = LocalDateTime.now();

        saveMetric(TEMPERATURE, payload.temperature(), now);
        saveMetric(HUMIDITY, payload.humidity(), now);
        saveMetric(LIGHT, payload.light(), now);

        // ESP32 con gui du lieu tuc la phan cung dang song
        hardwareStatus.setOnline(true);

        realtime.sendSensorUpdate(new Dtos.ChartPointDto(
                now, payload.temperature(), payload.humidity(), payload.light()));

        log.debug("[SENSOR] T={} H={} L={}", payload.temperature(), payload.humidity(), payload.light());
    }

    private void saveMetric(String category, Double value, LocalDateTime time) {
        if (value == null) {
            return;
        }
        ClassSensor sensor = sensorRepository.findByCategory(category).orElse(null);
        if (sensor == null) {
            log.warn("[SENSOR] Chua khai bao cam bien loai '{}' trong bang class_sensors", category);
            return;
        }
        EnvironmentalMetric metric = new EnvironmentalMetric();
        metric.setSensor(sensor);
        metric.setReadingValue(value);
        metric.setTimestamp(time);
        metricRepository.save(metric);
    }

    /** GET /api/sensors/latest - so hien tai + 12 diem gan nhat cho bieu do */
    @Transactional(readOnly = true)
    public Dtos.LatestSensorDto getLatest() {
        Double temperature = latestValue(TEMPERATURE);
        Double humidity = latestValue(HUMIDITY);
        Double light = latestValue(LIGHT);

        LocalDateTime timestamp = metricRepository
                .findFirstBySensor_CategoryOrderByTimestampDesc(TEMPERATURE)
                .map(EnvironmentalMetric::getTimestamp)
                .orElse(null);

        return new Dtos.LatestSensorDto(temperature, humidity, light, timestamp,
                hardwareStatus.isOnline(), buildHistory());
    }

    private Double latestValue(String category) {
        return metricRepository.findFirstBySensor_CategoryOrderByTimestampDesc(category)
                .map(EnvironmentalMetric::getReadingValue)
                .orElse(null);
    }

    /** Ghep 3 chuoi do thanh cac diem theo thoi gian, sap xep cu -> moi */
    private List<Dtos.ChartPointDto> buildHistory() {
        Pageable limit = PageRequest.of(0, CHART_POINTS);
        List<EnvironmentalMetric> temps = metricRepository
                .findBySensor_CategoryOrderByTimestampDesc(TEMPERATURE, limit);
        List<EnvironmentalMetric> hums = metricRepository
                .findBySensor_CategoryOrderByTimestampDesc(HUMIDITY, limit);
        List<EnvironmentalMetric> lights = metricRepository
                .findBySensor_CategoryOrderByTimestampDesc(LIGHT, limit);

        int size = Math.min(temps.size(), Math.min(hums.size(), lights.size()));
        List<Dtos.ChartPointDto> history = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            history.add(new Dtos.ChartPointDto(
                    temps.get(i).getTimestamp(),
                    temps.get(i).getReadingValue(),
                    hums.get(i).getReadingValue(),
                    lights.get(i).getReadingValue()));
        }
        Collections.reverse(history);
        return history;
    }

}
