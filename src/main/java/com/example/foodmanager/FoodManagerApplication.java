package com.example.foodmanager;

import jakarta.annotation.PostConstruct;
import java.util.TimeZone;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@EnableAsync
public class FoodManagerApplication {

    public static void main(String[] args) {
        SpringApplication.run(FoodManagerApplication.class, args);
    }

    // ▼▼▼ 追加: アプリ全体のタイムゾーンを日本時間(JST)に固定する ▼▼▼
    @PostConstruct
    public void init() {
        TimeZone.setDefault(TimeZone.getTimeZone("Asia/Tokyo"));
        System.out.println("アプリのタイムゾーンを Asia/Tokyo (日本時間) に設定しました");
    }
}