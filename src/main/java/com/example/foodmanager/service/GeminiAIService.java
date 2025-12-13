package com.example.foodmanager.service;

import com.example.foodmanager.model.Recipe;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;
import reactor.netty.transport.ProxyProvider;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;

import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class GeminiAIService {

    @Value("${gemini.api.key}")
    private String apiKey;

    @Value("${gemini.api.url}")
    private String apiUrl;

    private final WebClient webClient;
    private final ObjectMapper objectMapper;

    public GeminiAIService() {
        // プロキシ設定を試す
        HttpClient httpClient;

        // システムプロパティからプロキシ設定を確認
        String httpProxy = System.getProperty("http.proxyHost");
        String httpProxyPort = System.getProperty("http.proxyPort");
        String httpsProxy = System.getProperty("https.proxyHost");
        String httpsProxyPort = System.getProperty("https.proxyPort");

        System.out.println("=== プロキシ設定確認 ===");
        System.out.println("HTTP Proxy: " + httpProxy + ":" + httpProxyPort);
        System.out.println("HTTPS Proxy: " + httpsProxy + ":" + httpsProxyPort);

        if (httpsProxy != null && httpsProxyPort != null) {
            System.out.println("HTTPSプロキシを設定中...");
            httpClient = HttpClient.create()
                .proxy(proxy -> proxy.type(ProxyProvider.Proxy.HTTP)
                    .host(httpsProxy)
                    .port(Integer.parseInt(httpsProxyPort)));
        } else if (httpProxy != null && httpProxyPort != null) {
            System.out.println("HTTPプロキシを設定中...");
            httpClient = HttpClient.create()
                .proxy(proxy -> proxy.type(ProxyProvider.Proxy.HTTP)
                    .host(httpProxy)
                    .port(Integer.parseInt(httpProxyPort)));
        } else {
            System.out.println("プロキシ設定なしで接続を試みます");
            httpClient = HttpClient.create();
        }

        this.webClient = WebClient.builder()
            .clientConnector(new ReactorClientHttpConnector(httpClient))
            .build();
        this.objectMapper = new ObjectMapper();
    }

    public Recipe generateRecipeFromIngredients(List<String> ingredients) {
        System.out.println("=== Gemini API 呼び出し開始 ===");
        System.out.println("API Key: " + (apiKey != null && !apiKey.isEmpty() ? "設定済み" : "未設定"));
        System.out.println("API URL: " + apiUrl);

        if (apiKey == null || apiKey.isEmpty()) {
            System.out.println("APIキーが未設定のため、ダミーレシピを返します");
            return createDummyRecipe(ingredients);
        }

        try {
            System.out.println("API呼び出しを実行中...");
            String prompt = createPrompt(ingredients);

            // 無料プランのレート制限対応：少し待機
            Thread.sleep(1000); // 1秒待機

            String response = callGeminiAPI(prompt);
            System.out.println("API呼び出し成功、レスポンスを解析中...");
            Recipe result = parseRecipeFromResponse(response, ingredients);
            System.out.println("レシピ解析完了: " + result.getTitle());
            return result;
        } catch (Exception e) {
            System.err.println("Gemini API呼び出しエラー: " + e.getMessage());
            e.printStackTrace(); // スタックトレースも表示
            return createDummyRecipe(ingredients);
        }
    }

    private String createPrompt(List<String> ingredients) {
        String ingredientList = String.join("、", ingredients);
        return String.format(
            "以下の食材を使った美味しいレシピを1つ提案してください。\n" +
            "食材: %s\n\n" +
            "以下の形式で回答してください：\n" +
            "料理名: [料理名]\n" +
            "材料: [必要な材料をリスト形式で]\n" +
            "作り方: [手順を番号付きで]\n" +
            "調理時間: [分数]\n" +
            "難易度: [簡単/普通/難しい]",
            ingredientList
        );
    }

    private String callGeminiAPI(String prompt) {
        System.out.println("プロンプト: " + prompt);

        // ネットワーク接続テスト
        System.out.println("=== ネットワーク接続テスト ===");
        try {
            String testResponse = webClient.get()
                .uri("https://www.google.com")
                .retrieve()
                .bodyToMono(String.class)
                .block();
            System.out.println("Google接続テスト: 成功");
        } catch (Exception e) {
            System.err.println("Google接続テスト: 失敗 - " + e.getMessage());
            System.err.println("これはプロキシまたはネットワークの問題を示しています");
        }

        // Gemini API v1beta用のリクエスト形式
        Map<String, Object> requestBody = Map.of(
            "contents", List.of(
                Map.of(
                    "parts", List.of(
                        Map.of("text", prompt)
                    )
                )
            ),
            "generationConfig", Map.of(
                "temperature", 0.7,
                "topK", 1,
                "topP", 1,
                "maxOutputTokens", 2048
            )
        );

        System.out.println("API URL: " + apiUrl + "?key=" + apiKey.substring(0, 10) + "...");

        Mono<String> responseMono = webClient.post()
            .uri(apiUrl + "?key=" + apiKey)
            .header("Content-Type", "application/json")
            .header("Accept", "application/json")
            .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
            .bodyValue(requestBody)
            .retrieve()
            .onStatus(httpStatus -> httpStatus.is4xxClientError() || httpStatus.is5xxServerError(),
                clientResponse -> {
                    System.err.println("HTTP Error: " + clientResponse.statusCode());
                    return clientResponse.bodyToMono(String.class)
                        .map(errorBody -> {
                            System.err.println("Error response body: " + errorBody);
                            return new RuntimeException("API Error: " + clientResponse.statusCode() + " - " + errorBody);
                        });
                })
            .bodyToMono(String.class);

        String response = responseMono.block();
        System.out.println("API レスポンス: " + (response != null ? response.substring(0, Math.min(200, response.length())) + "..." : "null"));
        return response;
    }

    private Recipe parseRecipeFromResponse(String response, List<String> ingredients) {
        System.out.println("レスポンス解析開始...");
        try {
            if (response == null || response.trim().isEmpty()) {
                System.err.println("API レスポンスが空です");
                return createDummyRecipe(ingredients);
            }

            JsonNode rootNode = objectMapper.readTree(response);
            System.out.println("JSON解析成功");

            JsonNode candidatesNode = rootNode.get("candidates");
            if (candidatesNode == null) {
                System.err.println("candidates ノードが見つかりません");
                System.err.println("完全なレスポンス: " + response);
                return createDummyRecipe(ingredients);
            }

            if (candidatesNode != null && candidatesNode.isArray() && candidatesNode.size() > 0) {
                JsonNode contentNode = candidatesNode.get(0).get("content");
                JsonNode partsNode = contentNode.get("parts");

                if (partsNode != null && partsNode.isArray() && partsNode.size() > 0) {
                    String text = partsNode.get(0).get("text").asText();
                    return parseRecipeText(text);
                }
            }
        } catch (Exception e) {
            System.err.println("レスポンス解析エラー: " + e.getMessage());
        }

        return createDummyRecipe(ingredients);
    }

    private Recipe parseRecipeText(String text) {
        String[] lines = text.split("\n");
        String title = "提案レシピ";
        String ingredients = "";
        String instructions = "";
        String cookingTime = "30分";
        String difficulty = "普通";

        for (String line : lines) {
            line = line.trim();
            if (line.startsWith("料理名:") || line.startsWith("タイトル:")) {
                title = line.substring(line.indexOf(":") + 1).trim();
            } else if (line.startsWith("材料:") || line.startsWith("食材:")) {
                ingredients = line.substring(line.indexOf(":") + 1).trim();
            } else if (line.startsWith("作り方:") || line.startsWith("手順:")) {
                instructions = line.substring(line.indexOf(":") + 1).trim();
            } else if (line.startsWith("調理時間:")) {
                cookingTime = line.substring(line.indexOf(":") + 1).trim();
            } else if (line.startsWith("難易度:")) {
                difficulty = line.substring(line.indexOf(":") + 1).trim();
            } else if (line.matches("^\\d+\\..*")) {
                // 手順の続きの場合
                if (!instructions.isEmpty()) {
                    instructions += "\n";
                }
                instructions += line;
            }
        }

        return new Recipe(title, ingredients, instructions, cookingTime, difficulty);
    }

    private Recipe createDummyRecipe(List<String> ingredients) {
        System.out.println("ダミーレシピを作成します");
        String ingredientList = String.join("、", ingredients);
        return new Recipe(
            ingredients.get(0) + "を使った簡単料理",
            ingredientList + "など",
            "1. " + ingredients.get(0) + "を適当な大きさに切る\n" +
            "2. フライパンで炒める\n" +
            "3. 調味料で味を整える\n" +
            "4. 完成",
            "15分",
            "簡単"
        );
    }
}
