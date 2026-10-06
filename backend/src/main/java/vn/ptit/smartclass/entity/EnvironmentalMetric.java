package vn.ptit.smartclass.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/** Bang environmental_metrics - so lieu cam bien theo thoi gian (muc 3.3.2.4) */
@Entity
@Table(name = "environmental_metrics", indexes = {
        @Index(name = "idx_metric_time", columnList = "timestamp"),
        @Index(name = "idx_metric_sensor", columnList = "sensor_id")
})
public class EnvironmentalMetric {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sensor_id", nullable = false)
    private ClassSensor sensor;

    @Column(name = "reading_value", nullable = false)
    private Double readingValue;

    @Column(name = "timestamp", nullable = false)
    private LocalDateTime timestamp = LocalDateTime.now();

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public ClassSensor getSensor() { return sensor; }
    public void setSensor(ClassSensor sensor) { this.sensor = sensor; }

    public Double getReadingValue() { return readingValue; }
    public void setReadingValue(Double readingValue) { this.readingValue = readingValue; }

    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
}
