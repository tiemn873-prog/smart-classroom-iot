package vn.ptit.smartclass.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/** Bang smart_devices - thiet bi dieu khien (muc 3.3.2.2) */
@Entity
@Table(name = "smart_devices")
public class SmartDevice {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Ma phan cung, vd LIGHT_01 - trung voi device_code ben firmware */
    @Column(name = "device_code", length = 50, nullable = false, unique = true)
    private String deviceCode;

    @Column(name = "name", length = 100, nullable = false)
    private String name;

    @Column(name = "type", length = 50, nullable = false)
    private String type;

    /** ON hoac OFF - trang thai THUC TE do ESP32 bao ve */
    @Column(name = "current_state", length = 20, nullable = false)
    private String currentState = "OFF";

    @Column(name = "last_synced")
    private LocalDateTime lastSynced;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getDeviceCode() { return deviceCode; }
    public void setDeviceCode(String deviceCode) { this.deviceCode = deviceCode; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getCurrentState() { return currentState; }
    public void setCurrentState(String currentState) { this.currentState = currentState; }

    public LocalDateTime getLastSynced() { return lastSynced; }
    public void setLastSynced(LocalDateTime lastSynced) { this.lastSynced = lastSynced; }
}
