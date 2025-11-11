package org.example.updateplan;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class UpdatePlanApplication {

    public static void main(String[] args) {
        SpringApplication.run(UpdatePlanApplication.class, args);
    }

}
