package com.example.foodmanager.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

@Component
public class GeminiAPITester {

    @Value("${gemini.api.key}")
    private String apiKey;

    @Value("${gemini.api.url}")
    private String apiUrl;

    private final WebClient webClient = WebClient.builder().build();

    public void testAPIConnection() {
        System.out.println("=== Gemini API接続テスト ===");
        System.out.println("API Key: " + (apiKey != null && !apiKey.isEmpty() ? "設定済み (" + apiKey.substring(0, 10) + "...)" : "未設定"));
        System.out.println("API URL: " + apiUrl);

        if (apiKey == null || apiKey.isEmpty()) {
            System.err.println("APIキーが設定されていません");
            return;
        }

        try {
            Map<String, Object> testRequest = Map.of(
                "contents", List.of(
                    Map.of("parts", List.of(
                        Map.of("text", "Hello, can you respond with just 'API connection successful'?")
                    ))
                )
            );

            System.out.println("API接続テスト中...");

            Mono<String> responseMono = webClient.post()
                .uri(apiUrl + "?key=" + apiKey)
                .header("Content-Type", "application/json")
                .bodyValue(testRequest)
                .retrieve()
                .bodyToMono(String.class);

            String response = responseMono.block();
            System.out.println("✅ API接続成功！");
            System.out.println("レスポンス: " + (response != null ? response.substring(0, Math.min(100, response.length())) + "..." : "null"));

        } catch (Exception e) {
            System.err.println("❌ API接続失敗: " + e.getMessage());

            // 詳細なエラー情報
            if (e.getMessage().contains("404")) {
                System.err.println("推奨対処法: APIエンドポイントURLを確認してください");
            } else if (e.getMessage().contains("403") || e.getMessage().contains("401")) {
                System.err.println("推奨対処法: APIキーを確認してください");
            }
        }
    }
}
