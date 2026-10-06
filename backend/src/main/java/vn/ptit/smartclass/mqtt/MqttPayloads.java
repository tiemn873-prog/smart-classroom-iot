package vn.ptit.smartclass.mqtt;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/** Cau truc cac ban tin JSON ESP32 gui len, khop voi firmware smart_classroom_esp32.ino */
public final class MqttPayloads {

    private MqttPayloads() {
    }

    /** topic data/sensor:
     *  {"deviceCode":"ESP32_SMART_CLASS_01","temperature":26.9,"humidity":58.0,"light":410.5,"uptime":123456} */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record SensorPayload(String deviceCode, Double temperature, Double humidity,
                                Double light, Long uptime) {
    }

    /** topic device/status:
     *  {"deviceId":1,"deviceCode":"LIGHT_01","state":"ON","source":"USER","requestId":"15"} */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record StatusPayload(Long deviceId, String deviceCode, String state,
                                String source, String requestId) {
    }

    /** topic hardware/status:
     *  {"deviceCode":"ESP32_SMART_CLASS_01","status":"ONLINE"} */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record HardwarePayload(String deviceCode, String status) {
    }
}
