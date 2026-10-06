package vn.ptit.smartclass.entity;

import jakarta.persistence.*;

/** Bang class_sensors - danh muc cam bien (muc 3.3.2.3) */
@Entity
@Table(name = "class_sensors")
public class ClassSensor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Vd: TEMP_01, HUMI_01, LUX_01 */
    @Column(name = "sensor_code", length = 50, nullable = false, unique = true)
    private String sensorCode;

    /** temperature | humidity | light - dung lam khoa loc cho Frontend */
    @Column(name = "category", length = 50, nullable = false)
    private String category;

    @Column(name = "unit_label", length = 20, nullable = false)
    private String unitLabel;

    @Column(name = "is_active", nullable = false)
    private boolean active = true;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getSensorCode() { return sensorCode; }
    public void setSensorCode(String sensorCode) { this.sensorCode = sensorCode; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getUnitLabel() { return unitLabel; }
    public void setUnitLabel(String unitLabel) { this.unitLabel = unitLabel; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
}
