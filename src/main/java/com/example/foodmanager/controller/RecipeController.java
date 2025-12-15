package com.example.foodmanager.controller;

import com.example.foodmanager.model.Food;
import com.example.foodmanager.model.Recipe;
import com.example.foodmanager.model.SavedRecipe;
import com.example.foodmanager.model.User;
import com.example.foodmanager.repository.FoodRepository;
import com.example.foodmanager.repository.SavedRecipeRepository;
import com.example.foodmanager.repository.UserRepository;
import com.example.foodmanager.service.GeminiAIService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity; // 追加
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
    private SavedRecipeRepository savedRecipeRepository;

    @Autowired
    private GeminiAIService geminiAIService;

    private User getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return userRepository.findByEmail(auth.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    @GetMapping("/suggest")
    public String showRecipeSuggestion(Model model) {
        User currentUser = getCurrentUser();
        List<Food> myFoods = foodRepository.findByUser(currentUser);
        
        // ▼▼▼ 追加: 削除候補として表示するために、保存済みレシピ一覧も渡す ▼▼▼
        List<SavedRecipe> savedRecipes = savedRecipeRepository.findByUserOrderBySavedAtDesc(currentUser);
        model.addAttribute("savedRecipes", savedRecipes);

        model.addAttribute("foods", myFoods);
        return "recipe-suggest";
    }

    @PostMapping("/generate")
    @ResponseBody
    public Recipe generateRecipe(@RequestParam List<Long> selectedFoodIds) {
        User currentUser = getCurrentUser();

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

    @PostMapping("/save")
    public String saveRecipe(@ModelAttribute SavedRecipe recipe, Model model) {
        User currentUser = getCurrentUser();

        long count = savedRecipeRepository.countByUser(currentUser);
        if (count >= 10) {
            return "redirect:/?error=limit_reached";
        }

        recipe.setUser(currentUser);
        savedRecipeRepository.save(recipe);

        return "redirect:/?saved=true";
    }
    
    // 通常の削除（一覧画面用）
    @PostMapping("/delete")
    public String deleteRecipe(@RequestParam Long id) {
        User currentUser = getCurrentUser();
        savedRecipeRepository.findById(id)
                .filter(r -> r.getUser().equals(currentUser))
                .ifPresent(savedRecipeRepository::delete);
        return "redirect:/?tab=recipes";
    }

    // ▼▼▼ 追加: ページ遷移せずに削除するためのAPI（レシピ提案画面用） ▼▼▼
    @PostMapping("/delete/ajax")
    @ResponseBody
    public ResponseEntity<String> deleteRecipeAjax(@RequestParam Long id) {
        User currentUser = getCurrentUser();
        savedRecipeRepository.findById(id)
                .filter(r -> r.getUser().equals(currentUser))
                .ifPresent(savedRecipeRepository::delete);
        return ResponseEntity.ok("Deleted");
    }
}