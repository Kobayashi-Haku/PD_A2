package com.example.foodmanager.service;

import com.example.foodmanager.model.Recipe;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
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
        // プロキシ設定（大学環境などに対応）
        HttpClient httpClient;
        String httpProxy = System.getProperty("http.proxyHost");
        String httpProxyPort = System.getProperty("http.proxyPort");
        String httpsProxy = System.getProperty("https.proxyHost");
        String httpsProxyPort = System.getProperty("https.proxyPort");

        if (httpsProxy != null && httpsProxyPort != null) {
            httpClient = HttpClient.create()
                .proxy(proxy -> proxy.type(ProxyProvider.Proxy.HTTP)
                    .host(httpsProxy)
                    .port(Integer.parseInt(httpsProxyPort)));
        } else if (httpProxy != null && httpProxyPort != null) {
            httpClient = HttpClient.create()
                .proxy(proxy -> proxy.type(ProxyProvider.Proxy.HTTP)
                    .host(httpProxy)
                    .port(Integer.parseInt(httpProxyPort)));
        } else {
            httpClient = HttpClient.create();
        }

        this.webClient = WebClient.builder()
            .clientConnector(new ReactorClientHttpConnector(httpClient))
            .build();
        this.objectMapper = new ObjectMapper();
    }

    public Recipe generateRecipeFromIngredients(List<String> ingredients) {
        log.info("=== Gemini API 呼び出し開始 ===");
        
        // ▼▼▼ 修正: ダミーレシピではなく例外を投げる ▼▼▼
        if (apiKey == null || apiKey.isEmpty()) {
            log.error("APIキーが設定されていません");
            throw new RuntimeException("AIサービスの準備ができていません（APIキー未設定）");
        }

        try {
            String prompt = createPrompt(ingredients);
            // Thread.sleep(1000); // 必要に応じてコメントアウト解除（レート制限対策）

            String response = callGeminiAPI(prompt);
            return parseRecipeFromResponse(response);
            
        } catch (Exception e) {
            log.error("Gemini API呼び出しエラー", e);
            // ▼▼▼ 修正: ここでも例外を投げる ▼▼▼
            throw new RuntimeException("レシピの生成に失敗しました: " + e.getMessage());
        }
    }

    private String createPrompt(List<String> ingredients) {
        String ingredientList = String.join("、", ingredients);
        return """
            以下の食材を使った美味しいレシピを1つ提案してください。
            食材: %s
            
            以下の形式で回答してください：
            料理名: [料理名]
            材料: [必要な材料をリスト形式で]
            作り方: [手順を番号付きで]
            調理時間: [分数]
            難易度: [簡単/普通/難しい]
            """.formatted(ingredientList);
    }

    private String callGeminiAPI(String prompt) {
        Map<String, Object> requestBody = Map.of(
            "contents", List.of(
                Map.of("parts", List.of(Map.of("text", prompt)))
            ),
            "generationConfig", Map.of(
                "temperature", 0.7,
                "topK", 1,
                "topP", 1,
                "maxOutputTokens", 2048
            )
        );

        Mono<String> responseMono = webClient.post()
            .uri(apiUrl + "?key=" + apiKey)
            .header("Content-Type", "application/json")
            .bodyValue(requestBody)
            .retrieve()
            .bodyToMono(String.class);

        return responseMono.block();
    }

    private Recipe parseRecipeFromResponse(String response) {
        try {
            if (response == null || response.trim().isEmpty()) {
                throw new RuntimeException("AIからの応答が空でした");
            }

            JsonNode rootNode = objectMapper.readTree(response);
            JsonNode candidatesNode = rootNode.get("candidates");

            if (candidatesNode != null && candidatesNode.isArray() && candidatesNode.size() > 0) {
                JsonNode contentNode = candidatesNode.get(0).get("content");
                JsonNode partsNode = contentNode.get("parts");

                if (partsNode != null && partsNode.isArray() && partsNode.size() > 0) {
                    String text = partsNode.get(0).get("text").asText();
                    return parseRecipeText(text);
                }
            }
            throw new RuntimeException("AIからの応答の解析に失敗しました（形式エラー）");

        } catch (Exception e) {
            log.error("レスポンス解析エラー", e);
            throw new RuntimeException("レシピデータの読み取りに失敗しました");
        }
    }

    private Recipe parseRecipeText(String text) {
        String[] lines = text.split("\n");
        String title = "提案レシピ";
        String ingredients = "";
        String instructions = "";
        String cookingTime = "不明";
        String difficulty = "普通";

        for (String line : lines) {
            line = line.trim();
            // マークダウンの太字除去などのクリーニングを行っても良い
            line = line.replace("**", ""); 

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
            } else if (line.matches("^\\d+\\..*") || line.startsWith("- ")) {
                if (!instructions.isEmpty()) instructions += "\n";
                instructions += line;
            } else if (!line.isEmpty() && !line.contains(":")) {
                // 行頭にラベルがない場合、前の項目の続きとみなす簡易ロジック
                if (!ingredients.isEmpty() && instructions.isEmpty()) {
                     ingredients += "\n" + line;
                }
            }
        }
        return new Recipe(title, ingredients, instructions, cookingTime, difficulty);
    }
}