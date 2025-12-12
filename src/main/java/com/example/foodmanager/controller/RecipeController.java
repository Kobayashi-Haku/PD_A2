package com.example.foodmanager.controller;

import com.example.foodmanager.model.Food;
import com.example.foodmanager.model.Recipe;
import com.example.foodmanager.model.User;
import com.example.foodmanager.repository.FoodRepository;
import com.example.foodmanager.repository.UserRepository;
import com.example.foodmanager.service.GeminiAIService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/recipe")
public class RecipeController {

    @Autowired
    private FoodRepository foodRepository;

    @Autowired
    private UserRepository userRepository; // 追加: ユーザー特定のために必要

    @Autowired
    private GeminiAIService geminiAIService;

    // ヘルパーメソッド: ログイン中のユーザーを取得
    private User getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return userRepository.findByUsername(auth.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    @GetMapping("/suggest")
    public String showRecipeSuggestion(Model model) {
        // 修正: 全員分ではなく、ログインユーザーの食品だけを取得
        User currentUser = getCurrentUser();
        List<Food> myFoods = foodRepository.findByUser(currentUser);
        
        model.addAttribute("foods", myFoods);
        return "recipe-suggest";
    }

    @PostMapping("/generate")
    @ResponseBody
    public Recipe generateRecipe(@RequestParam List<Long> selectedFoodIds) {
        User currentUser = getCurrentUser();

        // 選択されたIDの食品を取得し、かつ「自分の食品であるもの」だけに絞り込む（セキュリティ対策）
        List<Food> selectedFoods = foodRepository.findAllById(selectedFoodIds).stream()
                .filter(food -> food.getUser().getId().equals(currentUser.getId()))
                .collect(Collectors.toList());

        if (selectedFoods.isEmpty()) {
            throw new RuntimeException("有効な食品が選択されていません");
        }

        List<String> ingredients = selectedFoods.stream()
                .map(Food::getName)
                .collect(Collectors.toList());

        return geminiAIService.generateRecipeFromIngredients(ingredients);
    }
}