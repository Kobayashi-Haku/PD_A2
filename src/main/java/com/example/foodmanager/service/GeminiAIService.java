package com.example.foodmanager.service;

import com.example.foodmanager.model.Recipe;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j; // 追加: ログ用
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
@Slf4j // 追加: これで log.info が使えるようになります
public class GeminiAIService {

    @Value("${gemini.api.key}")
    private String apiKey;

    @Value("${gemini.api.url}")
    private String apiUrl;

    private final WebClient webClient;
    private final ObjectMapper objectMapper;

    public GeminiAIService() {
        // プロキシ設定
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
        
        if (apiKey == null || apiKey.isEmpty()) {
            log.warn("APIキーが未設定のため、ダミーレシピを返します");
            return createDummyRecipe(ingredients);
        }

        try {
            String prompt = createPrompt(ingredients);
            Thread.sleep(1000); // レート制限対策

            String response = callGeminiAPI(prompt);
            return parseRecipeFromResponse(response, ingredients);
        } catch (Exception e) {
            log.error("Gemini API呼び出しエラー", e);
            return createDummyRecipe(ingredients);
        }
    }

    private String createPrompt(List<String> ingredients) {
        String ingredientList = String.join("、", ingredients);
        // テキストブロック（Java 15+）
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

    private Recipe parseRecipeFromResponse(String response, List<String> ingredients) {
        try {
            if (response == null || response.trim().isEmpty()) return createDummyRecipe(ingredients);

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
        } catch (Exception e) {
            log.error("レスポンス解析エラー", e);
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
                if (!instructions.isEmpty()) instructions += "\n";
                instructions += line;
            }
        }
        return new Recipe(title, ingredients, instructions, cookingTime, difficulty);
    }

    private Recipe createDummyRecipe(List<String> ingredients) {
        String ingredientList = String.join("、", ingredients);
        return new Recipe(
            ingredients.get(0) + "を使った簡単料理",
            ingredientList + "など",
            "1. " + ingredients.get(0) + "を適当な大きさに切る\n2. 炒める\n3. 完成",
            "15分",
            "簡単"
        );
    }
}