package com.lab.monitoring;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class LabMonitoringApplication {
    public static void main(String[] args) {
        SpringApplication.run(LabMonitoringApplication.class, args);
    }
}
