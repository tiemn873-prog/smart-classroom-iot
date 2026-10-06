package vn.ptit.smartclass.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import vn.ptit.smartclass.entity.ClassSensor;
import vn.ptit.smartclass.entity.SmartDevice;
import vn.ptit.smartclass.entity.UserProfile;
import vn.ptit.smartclass.repository.ClassSensorRepository;
import vn.ptit.smartclass.repository.SmartDeviceRepository;
import vn.ptit.smartclass.repository.UserProfileRepository;
import vn.ptit.smartclass.service.SensorService;

/**
 * Nap du lieu danh muc lan dau chay: 1 ho so, 2 thiet bi, 3 cam bien.
 * Chay lai nhieu lan van an toan vi co kiem tra ton tai truoc khi them.
 */
@Component
public class DataSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);

    private final UserProfileRepository profileRepository;
    private final SmartDeviceRepository deviceRepository;
    private final ClassSensorRepository sensorRepository;

    public DataSeeder(UserProfileRepository profileRepository,
                      SmartDeviceRepository deviceRepository,
                      ClassSensorRepository sensorRepository) {
        this.profileRepository = profileRepository;
        this.deviceRepository = deviceRepository;
        this.sensorRepository = sensorRepository;
    }

    @Override
    public void run(String... args) {
        seedProfile();
        seedDevice("LIGHT_01", "Đèn", "Chiếu sáng");
        seedDevice("FAN_01", "Quạt", "Làm mát");
        seedSensor("TEMP_01", SensorService.TEMPERATURE, "°C");
        seedSensor("HUMI_01", SensorService.HUMIDITY, "%");
        seedSensor("LUX_01", SensorService.LIGHT, "lux");
    }

    /* Ghi de ho so moi lan khoi dong. Khac cac bang kia o cho: day la thong
       tin tac gia, chi co dung mot ban ghi, sua trong ma nguon la chay lai
       la cap nhat - khong phai vao MySQL sua tay. */
    private void seedProfile() {
        UserProfile profile = profileRepository.findAll().stream().findFirst()
                .orElseGet(UserProfile::new);

        profile.setFullName("Nguyễn Dụng Tiềm");
        profile.setStudentCode("B23DCCN813");
        profile.setMajor("Sinh viên CNPM");
        profile.setClassCode("D23CNPM01");
        profile.setEmail("tiemn873@gmail.com");
        profile.setLocation("Hà Nội, Việt Nam");
        profile.setAvatarUrl("/anh-dai-dien.png");
        profile.setGithubUrl("https://github.com/tiemn873-prog/smart-classroom-iot");
        profile.setFigmaUrl("https://www.figma.com/design/T5ec1lS0OuP7KCTMGLSSbw/Codia-AI-Figma-to-code--Figma-to-full-stack-Web-and-Mobile-apps--Communaut%25C3%25A9-?node-id=0-1&p=f&t=oE5s76b7eYnkXDSG-0");
        profile.setReportPdfUrl("https://docs.google.com/document/d/10HwIWbWv6JUbPiqKqpipuQHJBSmcB52lpSm_Wpbzsgo/edit?tab=t.0");
        profile.setApiDocsUrl("https://tiemn873-1910835.postman.co/workspace/B23DCCN813-Nguy%E1%BB%85n-D%E1%BB%A5ng-Ti%E1%BB%81m's-W~ac3fbaf4-71ad-4756-8081-5f6322e100d1/request/53525606-aac5d2a7-53de-4194-bb08-d91536e8dc36?action=share&creator=53525606");
        profile.setBio("Sinh viên thực hiện đề tài Hệ thống quản lý lớp học thông minh");
        profileRepository.save(profile);
        log.info("[SEED] Da cap nhat ho so sinh vien");
    }

    private void seedDevice(String code, String name, String type) {
        if (deviceRepository.findByDeviceCode(code).isPresent()) {
            return;
        }
        SmartDevice device = new SmartDevice();
        device.setDeviceCode(code);
        device.setName(name);
        device.setType(type);
        device.setCurrentState("OFF");
        deviceRepository.save(device);
        log.info("[SEED] Da tao thiet bi {}", code);
    }

    private void seedSensor(String code, String category, String unit) {
        if (sensorRepository.findByCategory(category).isPresent()) {
            return;
        }
        ClassSensor sensor = new ClassSensor();
        sensor.setSensorCode(code);
        sensor.setCategory(category);
        sensor.setUnitLabel(unit);
        sensor.setActive(true);
        sensorRepository.save(sensor);
        log.info("[SEED] Da tao cam bien {}", code);
    }
}
