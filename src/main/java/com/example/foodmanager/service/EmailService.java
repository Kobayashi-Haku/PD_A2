package com.example.foodmanager.service;

import com.example.foodmanager.model.Food;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(name = "app.notification.enabled", havingValue = "true", matchIfMissing = true)
public class EmailService {

    // HTTPリクエストを送るためのクライアント
    private final WebClient.Builder webClientBuilder;

    @Value("${brevo.api.key:}")
    private String brevoApiKey;

    @Value("${mail.sender:}")
    private String senderEmail;

    /**
     * Brevo APIを使ってメールを送信する共通メソッド
     */
    private void sendEmailViaApi(String toEmail, String subject, String content) {
        if (brevoApiKey == null || brevoApiKey.isEmpty()) {
            log.warn("Brevo APIキーが設定されていません。メール送信をスキップします。");
            return;
        }

        try {
            // Brevo APIに送るデータ（JSON形式）を作成
            Map<String, Object> body = Map.of(
                "sender", Map.of("name", "食品管理アプリ", "email", senderEmail),
                "to", List.of(Map.of("email", toEmail)),
                "subject", subject,
                "textContent", content
            );

            // APIへのPOST送信
            String response = webClientBuilder.build().post()
                .uri("https://api.brevo.com/v3/smtp/email")
                .header("api-key", brevoApiKey)
                .header("Content-Type", "application/json")
                .bodyValue(body)
                .retrieve()
                .bodyToMono(String.class)
                .block(); 

            log.info("Brevo APIでメール送信成功: {}", toEmail);

        } catch (Exception e) {
            log.error("Brevo API送信失敗: {}", e.getMessage());
        }
    }

    @Async
    public void sendExpirationNotification(Food food) {
        String subject = "消費期限のお知らせ - " + food.getName();
        String messageText = String.format(
            "こんにちは%sさん\n\n" +
            "以下の食品の消費期限が近づいています。\n\n" +
            "■ 食品名: %s\n" +
            "■ 消費期限: %s\n\n" +
            "使い道に迷ったらホーム画面から「レシピ提案」を試してみてください！\n\n" +
            "食品管理システム",
            food.getUser().getUsername(),
            food.getName(),
            food.getExpirationDate().format(DateTimeFormatter.ofPattern("yyyy/MM/dd"))
        );

        sendEmailViaApi(food.getUser().getEmail(), subject, messageText);
    }
    
    // パスワードリセット用
    @Async
    public void sendPasswordResetEmail(String toEmail, String resetUrl) {
        String subject = "パスワード再設定のご案内";
        String messageText = "以下のリンクをクリックしてパスワードを再設定してください。\n" +
                    "（リンクの有効期限は24時間です）\n\n" +
                    resetUrl + "\n\n" +
                    "もしこのメールに心当たりがない場合は、無視してください。";
        
        sendEmailViaApi(toEmail, subject, messageText);
    }
}