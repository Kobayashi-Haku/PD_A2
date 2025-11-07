package com.example.foodmanager.controller;

import com.example.foodmanager.model.Food;
import com.example.foodmanager.model.Recipe;
import com.example.foodmanager.repository.FoodRepository;
import com.example.foodmanager.service.GeminiAIService;
import org.springframework.beans.factory.annotation.Autowired;
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
    private GeminiAIService geminiAIService;

    @GetMapping("/suggest")
    public String showRecipeSuggestion(Model model) {
        List<Food> allFoods = foodRepository.findAll();
        model.addAttribute("foods", allFoods);
        return "recipe-suggest";
    }

    @PostMapping("/generate")
    @ResponseBody
    public Recipe generateRecipe(@RequestParam List<Long> selectedFoodIds) {
        List<Food> selectedFoods = foodRepository.findAllById(selectedFoodIds);
        List<String> ingredients = selectedFoods.stream()
                .map(Food::getName)
                .collect(Collectors.toList());

        return geminiAIService.generateRecipeFromIngredients(ingredients);
    }
}
