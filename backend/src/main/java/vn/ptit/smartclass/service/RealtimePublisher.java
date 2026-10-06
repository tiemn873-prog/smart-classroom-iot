package vn.ptit.smartclass.service;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import vn.ptit.smartclass.dto.Dtos;

/**
 * Day du lieu xuong trinh duyet qua WebSocket (STOMP).
 * Frontend dang ky nhan o cac kenh: /topic/sensor, /topic/device, /topic/status
 */
@Service
public class RealtimePublisher {

    public static final String TOPIC_SENSOR = "/topic/sensor";
    public static final String TOPIC_DEVICE = "/topic/device";
    public static final String TOPIC_STATUS = "/topic/status";

    private final SimpMessagingTemplate messagingTemplate;

    public RealtimePublisher(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    /** Buoc 8 Hinh 3: day so do moi nhat len Dashboard */
    public void sendSensorUpdate(Dtos.ChartPointDto point) {
        messagingTemplate.convertAndSend(TOPIC_SENSOR, point);
    }

    /** Buoc 22 Hinh 4: bao trang thai thiet bi da doi */
    public void sendDeviceUpdate(Dtos.DeviceDto device) {
        messagingTemplate.convertAndSend(TOPIC_DEVICE, device);
    }

    /** Buoc 11-13 Hinh 3: bao phan cung ONLINE / OFFLINE */
    public void sendHardwareStatus(Dtos.HardwareStatusDto status) {
        messagingTemplate.convertAndSend(TOPIC_STATUS, status);
    }
}
