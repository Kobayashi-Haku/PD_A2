package com.example.foodmanager.service;

import com.example.foodmanager.model.Food;
import com.example.foodmanager.model.User;
import com.example.foodmanager.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async; // ★ここが重要：追加してください！
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(name = "app.notification.enabled", havingValue = "true", matchIfMissing = true)
public class EmailService {

    private final JavaMailSender mailSender;
    private final UserRepository userRepository;

    @Value("${spring.mail.username}")
    private String mailUsername;
    
    @Value("${spring.mail.password}")
    private String mailPassword;

    @PostConstruct
    public void init() {
        log.info("EmailService initialized with:");
        log.info("Mail username: {}", mailUsername);
        // パスワードはログに出さない（セキュリティのため桁数のみ）
        log.info("Mail password length: {}", mailPassword != null ? mailPassword.length() : 0);
    }

    /**
     * 送信者メールアドレスをデータベースから取得
     * 最初に登録されたユーザーのメールアドレスを使用
     */
    private String getFromEmailAddress() {
        Optional<User> firstUser = userRepository.findAll().stream().findFirst();
        if (firstUser.isPresent()) {
            return firstUser.get().getEmail();
        } else {
            // デフォルトとして設定されたメールアドレスを使用
            return mailUsername;
        }
    }

    // ▼▼▼ 非同期で実行（@Async） ▼▼▼
    @Async
    public void sendExpirationNotification(Food food) {
        try {
            String fromEmail = getFromEmailAddress();
            
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(food.getUser().getEmail());
            message.setSubject("食品の賞味期限通知 - " + food.getName());
            
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
            
            message.setText(messageText);
            
            mailSender.send(message);
            log.info("通知メールを送信しました - ユーザー: {}, 食品: {}", 
                    food.getUser().getUsername(), food.getName());
            
        } catch (Exception e) {
            log.error("メール送信に失敗しました - ユーザー: {}, 食品: {}", 
                    food.getUser().getUsername(), food.getName(), e);
        }
    }

    // ▼▼▼ 非同期で実行（@Async） ▼▼▼
    @Async
    public void sendImmediateExpirationNotification(Food food) {
        try {
            String fromEmail = getFromEmailAddress();
            
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(food.getUser().getEmail());
            message.setSubject("【緊急】明日が賞味期限！ - " + food.getName());
            
            String messageText = String.format(
                "こんにちは、%sさん\n\n" +
                "登録された食品の賞味期限が明日です！\n\n" +
                "食品名: %s\n" +
                "賞味期限: %s（明日）\n" +
                "登録日時: %s\n\n" +
                "緊急！明日が賞味期限です。お早めにお召し上がりください。\n\n" +
                "食品管理システム",
                food.getUser().getUsername(),
                food.getName(),
                food.getExpirationDate().format(DateTimeFormatter.ofPattern("yyyy年MM月dd日")),
                food.getRegisteredAt().format(DateTimeFormatter.ofPattern("yyyy年MM月dd日 HH:mm"))
            );
            
            message.setText(messageText);
            
            mailSender.send(message);
            log.info("緊急通知メールを送信しました - ユーザー: {}, 食品: {}", 
                    food.getUser().getUsername(), food.getName());
            
        } catch (Exception e) {
            log.error("緊急メール送信に失敗しました - ユーザー: {}, 食品: {}", 
                    food.getUser().getUsername(), food.getName(), e);
        }
    }
}