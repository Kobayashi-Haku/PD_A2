package com.example.foodmanager.controller;

import com.example.foodmanager.model.Food;
import com.example.foodmanager.model.Recipe;
import com.example.foodmanager.model.SavedRecipe; // 追加
import com.example.foodmanager.model.User;
import com.example.foodmanager.repository.FoodRepository;
import com.example.foodmanager.repository.SavedRecipeRepository; // 追加
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
    private UserRepository userRepository;

    @Autowired
    private SavedRecipeRepository savedRecipeRepository; // ★ここが抜けていました！

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
        User currentUser = getCurrentUser();
        List<Food> myFoods = foodRepository.findByUser(currentUser);
        
        model.addAttribute("foods", myFoods);
        return "recipe-suggest";
    }

    @PostMapping("/generate")
    @ResponseBody
    public Recipe generateRecipe(@RequestParam List<Long> selectedFoodIds) {
        User currentUser = getCurrentUser();

        // 選択されたIDの食品を取得し、かつ「自分の食品であるもの」だけに絞り込む
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

    // ▼▼▼ 追加: レシピを保存する処理 ▼▼▼
    @PostMapping("/save")
    public String saveRecipe(@ModelAttribute SavedRecipe recipe, Model model) {
        User currentUser = getCurrentUser();

        // 10件制限のチェック
        long count = savedRecipeRepository.countByUser(currentUser);
        if (count >= 10) {
            return "redirect:/?error=limit_reached";
        }

        recipe.setUser(currentUser);
        savedRecipeRepository.save(recipe);

        return "redirect:/?saved=true";
    }
    
    // ▼▼▼ 追加: レシピを削除する処理 ▼▼▼
    @PostMapping("/delete")
    public String deleteRecipe(@RequestParam Long id) {
        User currentUser = getCurrentUser();
        savedRecipeRepository.findById(id)
                .filter(r -> r.getUser().equals(currentUser))
                .ifPresent(savedRecipeRepository::delete);
        return "redirect:/?tab=recipes";
    }
}