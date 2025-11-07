package com.example.foodmanager.service;

import com.example.foodmanager.model.Recipe;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

@Service
public class GeminiAIService {

    @Value("${gemini.api.key}")
    private String apiKey;

    @Value("${gemini.api.url}")
    private String apiUrl;

    private final WebClient webClient;
    private final ObjectMapper objectMapper;

    public GeminiAIService() {
        this.webClient = WebClient.builder().build();
        this.objectMapper = new ObjectMapper();
    }

    public Recipe generateRecipeFromIngredients(List<String> ingredients) {
        if (apiKey == null || apiKey.isEmpty()) {
            return createDummyRecipe(ingredients);
        }

        try {
            String prompt = createPrompt(ingredients);
            String response = callGeminiAPI(prompt);
            return parseRecipeFromResponse(response, ingredients);
        } catch (Exception e) {
            System.err.println("Gemini API呼び出しエラー: " + e.getMessage());
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
        Map<String, Object> requestBody = Map.of(
            "contents", List.of(
                Map.of("parts", List.of(
                    Map.of("text", prompt)
                ))
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
