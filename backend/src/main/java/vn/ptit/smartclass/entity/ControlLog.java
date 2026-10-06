package vn.ptit.smartclass.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/** Bang control_logs - nhat ky dieu khien, theo doi ket qua tung lenh (muc 3.3.2.5) */
@Entity
@Table(name = "control_logs", indexes = {
        @Index(name = "idx_log_time", columnList = "requested_at")
})
public class ControlLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "device_id", nullable = false)
    private SmartDevice device;

    /** TURN_ON hoac TURN_OFF */
    @Column(name = "action_command", length = 20, nullable = false)
    private String actionCommand;

    /** PENDING -> SUCCESS hoac TIMEOUT */
    @Column(name = "sync_status", length = 20, nullable = false)
    private String syncStatus = "PENDING";

    @Column(name = "requested_at", nullable = false)
    private LocalDateTime requestedAt = LocalDateTime.now();

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public SmartDevice getDevice() { return device; }
    public void setDevice(SmartDevice device) { this.device = device; }

    public String getActionCommand() { return actionCommand; }
    public void setActionCommand(String actionCommand) { this.actionCommand = actionCommand; }

    public String getSyncStatus() { return syncStatus; }
    public void setSyncStatus(String syncStatus) { this.syncStatus = syncStatus; }

    public LocalDateTime getRequestedAt() { return requestedAt; }
    public void setRequestedAt(LocalDateTime requestedAt) { this.requestedAt = requestedAt; }

    public LocalDateTime getCompletedAt() { return completedAt; }
    public void setCompletedAt(LocalDateTime completedAt) { this.completedAt = completedAt; }
}
