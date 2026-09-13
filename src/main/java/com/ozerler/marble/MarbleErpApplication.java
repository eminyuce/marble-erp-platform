package com.ozerler.marble;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class MarbleErpApplication {

    public static void main(String[] args) {
        SpringApplication.run(MarbleErpApplication.class, args);
    }
}
