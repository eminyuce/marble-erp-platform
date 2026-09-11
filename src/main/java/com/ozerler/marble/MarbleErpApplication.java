package com.ozerler.marble;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@SpringBootApplication
@EnableJpaAuditing
public class MarbleErpApplication {

    public static void main(String[] args) {
        SpringApplication.run(MarbleErpApplication.class, args);
    }
}
