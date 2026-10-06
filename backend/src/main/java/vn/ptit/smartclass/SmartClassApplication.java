package vn.ptit.smartclass;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class SmartClassApplication {

    public static void main(String[] args) {
        SpringApplication.run(SmartClassApplication.class, args);
    }
}
