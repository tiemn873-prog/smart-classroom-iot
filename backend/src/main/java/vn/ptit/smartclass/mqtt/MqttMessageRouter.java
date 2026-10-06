package vn.ptit.smartclass.mqtt;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import vn.ptit.smartclass.service.DeviceControlService;
import vn.ptit.smartclass.service.SensorService;

/** Doc ban tin MQTT, chuyen JSON thanh doi tuong roi giao cho service tuong ung. */
@Component
public class MqttMessageRouter {

    private static final Logger log = LoggerFactory.getLogger(MqttMessageRouter.class);

    private final ObjectMapper objectMapper;
    private final SensorService sensorService;
    private final DeviceControlService deviceControlService;

    public MqttMessageRouter(ObjectMapper objectMapper,
                             SensorService sensorService,
                             DeviceControlService deviceControlService) {
        this.objectMapper = objectMapper;
        this.sensorService = sensorService;
        this.deviceControlService = deviceControlService;
    }

    @EventListener
    public void onMqttMessage(MqttMessageEvent event) {
        try {
            switch (event.topic()) {
                case "data/sensor" -> sensorService.handleSensorReading(
                        objectMapper.readValue(event.payload(), MqttPayloads.SensorPayload.class));

                case "device/status" -> deviceControlService.handleStatus(
                        objectMapper.readValue(event.payload(), MqttPayloads.StatusPayload.class));

                case "hardware/status" -> deviceControlService.handleHardwareStatus(
                        objectMapper.readValue(event.payload(), MqttPayloads.HardwarePayload.class));

                default -> log.debug("[MQTT] Bo qua topic khong dung den: {}", event.topic());
            }
        } catch (Exception e) {
            log.error("[MQTT] Ban tin '{}' khong doc duoc: {} | noi dung: {}",
                    event.topic(), e.getMessage(), event.payload());
        }
    }
}
