package ac.su.kdt.beusermanagementservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

// 스케줄링 기능을 사용하기 위한
@EnableScheduling
@SpringBootApplication
public class BeUserManagementServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(BeUserManagementServiceApplication.class, args);
    }

}
