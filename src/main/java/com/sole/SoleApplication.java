package com.sole;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@EnableJpaAuditing
@SpringBootApplication
public class SoleApplication {

    public static void main(String[] args) {
        SpringApplication.run(SoleApplication.class, args);
    }

}
