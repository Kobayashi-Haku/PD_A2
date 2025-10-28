package com.example.foodmanager;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class FoodManagerApplication {
    public static void main(String[] args) {
        SpringApplication.run(FoodManagerApplication.class, args);
    }
}