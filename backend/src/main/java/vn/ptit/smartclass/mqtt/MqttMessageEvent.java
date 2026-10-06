package vn.ptit.smartclass.mqtt;

/**
 * Su kien noi bo phat ra moi khi nhan duoc mot ban tin MQTT.
 * Dung su kien thay vi goi truc tiep de tranh phu thuoc vong tron
 * giua MqttService (gui lenh) va cac service xu ly (nhan lenh).
 */
public record MqttMessageEvent(String topic, String payload) {
}
