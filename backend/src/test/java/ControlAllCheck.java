import java.lang.reflect.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;
import vn.ptit.smartclass.controller.ApiController;
import vn.ptit.smartclass.dto.Dtos;
import vn.ptit.smartclass.entity.*;
import vn.ptit.smartclass.mqtt.*;
import vn.ptit.smartclass.repository.*;
import vn.ptit.smartclass.service.*;
import org.springframework.web.server.ResponseStatusException;

/** Standalone regression checks using in-memory repositories and MQTT, without hardware. */
public class ControlAllCheck {
    static <T> T proxy(Class<T> type, InvocationHandler handler) {
        return type.cast(Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type}, handler));
    }
    static void check(boolean condition) {
        if (!condition) throw new AssertionError();
    }
    static class Fixture {
        final Map<Long, SmartDevice> devices = new ConcurrentHashMap<>();
        final Map<Long, ControlLog> logs = new ConcurrentHashMap<>();
        final AtomicLong ids = new AtomicLong();
        final Set<Long> responding;
        final DeviceControlService service;
        final ApiController controller;
        Fixture(Set<Long> responding) {
            this.responding = responding;
            for (long id = 1; id <= 2; id++) {
                SmartDevice device = new SmartDevice();
                device.setId(id);
                device.setDeviceCode("TEST_" + id);
                device.setCurrentState("OFF");
                devices.put(id, device);
            }
            var repo = proxy(SmartDeviceRepository.class, (p,m,a) -> switch(m.getName()) {
                case "findAllByOrderByIdAsc" -> devices.values().stream().sorted(Comparator.comparing(SmartDevice::getId)).toList();
                case "findById" -> Optional.ofNullable(devices.get(a[0]));
                case "save" -> a[0];
                default -> throw new UnsupportedOperationException(m.getName());
            });
            var logRepo = proxy(ControlLogRepository.class, (p,m,a) -> {
                if (m.getName().equals("findById")) return Optional.ofNullable(logs.get(a[0]));
                if (m.getName().equals("save")) {
                    ControlLog entry = (ControlLog)a[0];
                    if (entry.getId() == null) {
                        Field id = ControlLog.class.getDeclaredField("id");
                        id.setAccessible(true);
                        id.set(entry, ids.incrementAndGet());
                    }
                    logs.put(entry.getId(), entry);
                    return entry;
                }
                throw new UnsupportedOperationException(m.getName());
            });
            MqttService mqtt = new MqttService(null,"","","","") {
                @Override public void publish(String topic, String payload) {
                    long id = Long.parseLong(topic.substring(topic.lastIndexOf('/')+1));
                    String request = payload.replaceAll(".*\"requestId\":\"([0-9]+)\".*", "$1");
                    if (Fixture.this.responding.contains(id)) {
                        String state = payload.contains("TURN_ON") ? "ON" : "OFF";
                        Fixture.this.service.handleStatus(new MqttPayloads.StatusPayload(id,"TEST_"+id,state,"USER",request));
                    }
                }
            };
            RealtimePublisher realtime = new RealtimePublisher(null) {
                @Override public void sendDeviceUpdate(Dtos.DeviceDto dto) {}
            };
            service = new DeviceControlService(repo,logRepo,mqtt,realtime,new HardwareStatusHolder(),40);
            controller = new ApiController(null,null,service);
        }
    }
    public static void main(String[] args) {
        Fixture success = new Fixture(Set.of(1L,2L));
        var on = success.controller.controlAll(new Dtos.ControlAllRequest("ON"));
        check(on.getStatusCode().value()==200 && on.getBody().success());
        check(on.getBody().data().size()==2 && success.logs.size()==2);
        check(on.getBody().data().stream().allMatch(r -> r.state().equals("ON") && r.status().equals("SUCCESS")));
        check(on.getBody().data().get(0).logId()!=on.getBody().data().get(1).logId());
        var off = success.controller.controlAll(new Dtos.ControlAllRequest("OFF"));
        check(off.getBody().data().stream().allMatch(r -> r.state().equals("OFF")));
        Fixture mixed = new Fixture(Set.of(1L));
        var partial = mixed.controller.controlAll(new Dtos.ControlAllRequest("ON"));
        check(partial.getStatusCode().value()==503 && !partial.getBody().success());
        check(partial.getBody().data().get(0).status().equals("SUCCESS"));
        check(partial.getBody().data().get(1).status().equals("TIMEOUT"));
        check(mixed.devices.get(1L).getCurrentState().equals("ON"));
        check(mixed.devices.get(2L).getCurrentState().equals("OFF"));
        Fixture timeout = new Fixture(Set.of());
        var failed = timeout.controller.controlAll(new Dtos.ControlAllRequest("OFF"));
        check(failed.getStatusCode().value()==503);
        check(timeout.logs.values().stream().allMatch(l -> l.getSyncStatus().equals("TIMEOUT")));
        for (String action : Arrays.asList(null,"INVALID")) {
            try {
                timeout.controller.controlAll(new Dtos.ControlAllRequest(action));
                throw new AssertionError();
            } catch (ResponseStatusException e) { check(e.getStatusCode().value()==400); }
        }
        check(timeout.logs.size()==2);
        System.out.println("PASS: bulk ON/OFF, unique logs, mixed success/timeout, all timeout, invalid input.");
    }
}
