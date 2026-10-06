package vn.ptit.smartclass.mqtt;

import jakarta.annotation.PreDestroy;
import org.eclipse.paho.client.mqttv3.*;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;

/**
 * Lop mang: ket noi toi Mosquitto, dang ky nhan cac topic tu ESP32
 * va gui lenh dieu khien xuong ESP32.
 */
@Service
public class MqttService {

    private static final Logger log = LoggerFactory.getLogger(MqttService.class);

    /** Cac topic ESP32 gui len, Backend can nghe */
    private static final String[] SUBSCRIBED_TOPICS = {
            "data/sensor", "device/status", "hardware/status"
    };

    private final ApplicationEventPublisher events;
    private final String brokerUrl;
    private final String username;
    private final String password;
    private final String clientId;

    private MqttClient client;

    public MqttService(ApplicationEventPublisher events,
                       @Value("${mqtt.broker-url}") String brokerUrl,
                       @Value("${mqtt.username}") String username,
                       @Value("${mqtt.password}") String password,
                       @Value("${mqtt.client-id}") String clientId) {
        this.events = events;
        this.brokerUrl = brokerUrl;
        this.username = username;
        this.password = password;
        this.clientId = clientId;
    }

    /**
     * Thu ket noi lai moi 5 giay. Nho vay Backend van khoi dong duoc khi broker chua bat,
     * va tu noi lai khi broker bat len sau.
     */
    @Scheduled(initialDelay = 1000, fixedDelay = 5000)
    public void ensureConnected() {
        if (client != null && client.isConnected()) {
            return;
        }
        try {
            if (client == null) {
                client = new MqttClient(brokerUrl, clientId, new MemoryPersistence());
                client.setCallback(new InboundCallback());
            }

            MqttConnectOptions options = new MqttConnectOptions();
            options.setUserName(username);
            options.setPassword(password.toCharArray());
            options.setCleanSession(true);
            options.setAutomaticReconnect(true);
            options.setConnectionTimeout(5);
            options.setKeepAliveInterval(20);

            // Khong dang ky topic o day nua: viec do da chuyen sang connectComplete(),
            // de lan tu dong noi lai cung duoc dang ky day du.
            client.connect(options);
        } catch (MqttException e) {
            log.warn("[MQTT] Chua ket noi duoc {} ({}). Se thu lai sau 5 giay.", brokerUrl, e.getMessage());
        }
    }

    /** Dang ky nhan tat ca topic ma ESP32 gui len */
    private void subscribeAll() {
        try {
            for (String topic : SUBSCRIBED_TOPICS) {
                client.subscribe(topic, 1);
            }
        } catch (MqttException e) {
            log.error("[MQTT] Dang ky topic that bai: {}", e.getMessage());
        }
    }

    /** Gui mot ban tin xuong broker */
    public void publish(String topic, String payload) {
        if (client == null || !client.isConnected()) {
            log.warn("[MQTT] Khong gui duoc '{}' vi chua ket noi broker", topic);
            return;
        }
        try {
            MqttMessage message = new MqttMessage(payload.getBytes(StandardCharsets.UTF_8));
            message.setQos(1);
            client.publish(topic, message);
            log.info("[MQTT] >> {} {}", topic, payload);
        } catch (MqttException e) {
            log.error("[MQTT] Loi khi gui '{}': {}", topic, e.getMessage());
        }
    }

    public boolean isConnected() {
        return client != null && client.isConnected();
    }

    @PreDestroy
    public void disconnect() {
        try {
            if (client != null && client.isConnected()) {
                client.disconnect();
            }
        } catch (MqttException e) {
            log.warn("[MQTT] Loi khi ngat ket noi: {}", e.getMessage());
        }
    }

    /** Nhan ban tin tu broker roi phat thanh su kien Spring cho cac service xu ly */
    private class InboundCallback implements MqttCallbackExtended {

        /**
         * Chay moi lan ket noi hoan tat, KE CA lan tu dong noi lai.
         *
         * Bat buoc dang ky lai topic o day: dang dung cleanSession = true nen
         * broker xoa sach dang ky cu moi lan mat ket noi. Neu chi dang ky luc
         * ket noi lan dau thi sau khi broker khoi dong lai, Backend van hien
         * "da ket noi" nhung khong nhan duoc ban tin nao nua.
         */
        @Override
        public void connectComplete(boolean reconnect, String serverURI) {
            subscribeAll();
            log.info("[MQTT] {} {} va da dang ky {} topic",
                    reconnect ? "Da TU NOI LAI" : "Da ket noi", serverURI, SUBSCRIBED_TOPICS.length);
        }

        @Override
        public void connectionLost(Throwable cause) {
            log.warn("[MQTT] Mat ket noi broker: {}", cause.getMessage());
        }

        @Override
        public void messageArrived(String topic, MqttMessage message) {
            String payload = new String(message.getPayload(), StandardCharsets.UTF_8);
            log.debug("[MQTT] << {} {}", topic, payload);
            try {
                events.publishEvent(new MqttMessageEvent(topic, payload));
            } catch (Exception e) {
                log.error("[MQTT] Loi khi xu ly ban tin tu '{}': {}", topic, e.getMessage(), e);
            }
        }

        @Override
        public void deliveryComplete(IMqttDeliveryToken token) {
            // khong can xu ly
        }
    }
}
