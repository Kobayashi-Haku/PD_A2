package com.example.foodmanager.controller;

import com.example.foodmanager.model.Food;
import com.example.foodmanager.model.User;
import com.example.foodmanager.repository.FoodRepository;
import com.example.foodmanager.repository.SavedRecipeRepository; // 追加
import com.example.foodmanager.repository.UserRepository;
import com.example.foodmanager.service.EmailService;
import com.example.foodmanager.service.MockEmailService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@Controller
@RequestMapping("/")
@Slf4j
public class FoodController {
    private final FoodRepository foodRepository;
    private final UserRepository userRepository;
    private final SavedRecipeRepository savedRecipeRepository; // 追加
    
    @Autowired(required = false)
    private EmailService emailService;
    
    @Autowired(required = false)
    private MockEmailService mockEmailService;
    
    @Value("${app.notification.enabled:false}")
    private boolean notificationEnabled;

    // コンストラクタを修正（SavedRecipeRepositoryを追加）
    public FoodController(FoodRepository foodRepository, UserRepository userRepository, SavedRecipeRepository savedRecipeRepository) {
        this.foodRepository = foodRepository;
        this.userRepository = userRepository;
        this.savedRecipeRepository = savedRecipeRepository;
    }

    private User getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return userRepository.findByUsername(auth.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    @GetMapping
    public String index(Model model) {
        User currentUser = getCurrentUser();
        LocalDate now = LocalDate.now();
        LocalDate threeDaysLater = now.plusDays(3);

        var foods = foodRepository.findByUserOrderByExpirationDateAsc(currentUser);
        var warning = foodRepository.findByUserAndExpirationDateBetween(currentUser, now, threeDaysLater);
        var expired = foodRepository.findByUserAndExpirationDateBefore(currentUser, now);

        // ▼▼▼ ここが重要：保存したレシピを取得して画面に渡す ▼▼▼
        var savedRecipes = savedRecipeRepository.findByUserOrderBySavedAtDesc(currentUser);
        model.addAttribute("savedRecipes", savedRecipes);
        // ▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲▲

        model.addAttribute("foods", foods);
        model.addAttribute("count", foods.size());
        model.addAttribute("warning", warning.size());
        model.addAttribute("expired", expired.size());
        return "list";
    }

    @GetMapping("/add")
    public String addForm() {
        return "form";
    }

    @PostMapping("/add")
    public String addSubmit(@RequestParam String name, @RequestParam String expirationDate) {
        User currentUser = getCurrentUser();
        Food food = new Food();
        food.setName(name);
        food.setExpirationDate(LocalDate.parse(expirationDate));
        food.setUser(currentUser);
        
        Food savedFood = foodRepository.save(food);
        
        // 即座通知ロジック
        LocalDate tomorrow = LocalDate.now().plusDays(1);
        if (savedFood.getExpirationDate().equals(tomorrow)) {
            try {
                if (notificationEnabled && emailService != null) {
                    emailService.sendImmediateExpirationNotification(savedFood);
                } else if (mockEmailService != null) {
                    mockEmailService.sendImmediateExpirationNotification(savedFood);
                }
                savedFood.setNotificationSent(true);
                foodRepository.save(savedFood);
            } catch (Exception e) {
                log.error("即座通知失敗", e);
            }
        }
        
        return "redirect:/";
    }

    @GetMapping("/edit/{id}")
    public String editForm(@PathVariable Long id, Model model) {
        User currentUser = getCurrentUser();
        Food food = foodRepository.findById(id)
                .filter(f -> f.getUser().equals(currentUser))
                .orElseThrow(() -> new RuntimeException("Food not found or access denied"));
        
        model.addAttribute("food", food);
        return "edit";
    }

    @PostMapping("/update")
    public String update(@RequestParam Long id, 
                        @RequestParam String name, 
                        @RequestParam String expirationDate) {
        User currentUser = getCurrentUser();
        Food food = foodRepository.findById(id)
                .filter(f -> f.getUser().equals(currentUser))
                .orElseThrow(() -> new RuntimeException("Food not found or access denied"));
        
        food.setName(name);
        food.setExpirationDate(LocalDate.parse(expirationDate));
        
        // 更新時の即座通知ロジック
        LocalDate tomorrow = LocalDate.now().plusDays(1);
        if (food.getExpirationDate().equals(tomorrow) && !food.isNotificationSent()) {
            try {
                if (notificationEnabled && emailService != null) {
                    emailService.sendImmediateExpirationNotification(food);
                } else if (mockEmailService != null) {
                    mockEmailService.sendImmediateExpirationNotification(food);
                }
                food.setNotificationSent(true);
            } catch (Exception e) {
                log.error("更新時即座通知失敗", e);
            }
        }
        
        foodRepository.save(food);
        return "redirect:/";
    }

    @PostMapping("/delete")
    public String delete(@RequestParam Long id) {
        User currentUser = getCurrentUser();
        foodRepository.findById(id)
                .filter(food -> food.getUser().equals(currentUser))
                .ifPresent(foodRepository::delete);
        return "redirect:/";
    }
}