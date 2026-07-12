package com.fabriciosanches.adamodulo3.faturas;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class MsFaturasApplication {

    public static void main(String[] args) {
        SpringApplication.run(MsFaturasApplication.class, args);
    }
}
