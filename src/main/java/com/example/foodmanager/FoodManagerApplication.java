package com.example.foodmanager;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync; // ▼ 追加
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@EnableAsync // ▼ 追加: これで裏側での処理が可能になります
public class FoodManagerApplication {
    public static void main(String[] args) {
        SpringApplication.run(FoodManagerApplication.class, args);
    }
}