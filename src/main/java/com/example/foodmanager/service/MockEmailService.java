package com.example.foodmanager.service;

import com.example.foodmanager.model.Food;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.time.format.DateTimeFormatter;

/**
 * 開発・テスト用のメールサービス
 * 実際にメールを送信せず、ログに出力のみ行う
 */
@Service
@ConditionalOnProperty(name = "app.notification.enabled", havingValue = "false", matchIfMissing = true)
public class MockEmailService {

    private static final Logger log = LoggerFactory.getLogger(MockEmailService.class);

    public void sendExpirationNotification(Food food) {
        log.info("=== モックメール送信 ===");
        log.info("宛先: {}", food.getUser().getEmail());
        log.info("件名: 食品の賞味期限通知 - {}", food.getName());
        
        String messageText = String.format(
            "こんにちは、%sさん\n\n" +
            "登録された食品の賞味期限が近づいています。\n\n" +
            "食品名: %s\n" +
            "賞味期限: %s\n" +
            "登録日時: %s\n\n" +
            "お早めにお召し上がりください。\n\n" +
            "食品管理システム",
            food.getUser().getUsername(),
            food.getName(),
            food.getExpirationDate().format(DateTimeFormatter.ofPattern("yyyy年MM月dd日")),
            food.getRegisteredAt().format(DateTimeFormatter.ofPattern("yyyy年MM月dd日 HH:mm"))
        );
        
        log.info("本文:\n{}", messageText);
        log.info("=== メール送信完了（モック） ===");
    }

    public void sendImmediateExpirationNotification(Food food) {
        log.info("=== 緊急モックメール送信 ===");
        log.info("宛先: {}", food.getUser().getEmail());
        log.info("件名: 【緊急】明日が賞味期限！ - {}", food.getName());
        
        String messageText = String.format(
            "こんにちは、%sさん\n\n" +
            "登録された食品の賞味期限が明日です！\n\n" +
            "食品名: %s\n" +
            "賞味期限: %s（明日）\n" +
            "登録日時: %s\n\n" +
            "緊急！明日が賞味期限です。今日中にお召し上がりください。\n\n" +
            "食品管理システム",
            food.getUser().getUsername(),
            food.getName(),
            food.getExpirationDate().format(DateTimeFormatter.ofPattern("yyyy年MM月dd日")),
            food.getRegisteredAt().format(DateTimeFormatter.ofPattern("yyyy年MM月dd日 HH:mm"))
        );
        
        log.info("本文:\n{}", messageText);
        log.info("=== 緊急メール送信完了（モック） ===");
    }
}