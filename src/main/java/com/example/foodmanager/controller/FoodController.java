package com.example.foodmanager.controller;

import com.example.foodmanager.model.Food;
import com.example.foodmanager.model.User;
import com.example.foodmanager.repository.FoodRepository;
import com.example.foodmanager.repository.SavedRecipeRepository;
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
import java.time.temporal.ChronoUnit;

@Controller
@RequestMapping("/")
@Slf4j
public class FoodController {

    private final FoodRepository foodRepository;
    private final UserRepository userRepository;
    private final SavedRecipeRepository savedRecipeRepository;

    @Autowired(required = false)
    private EmailService emailService;

    @Autowired(required = false)
    private MockEmailService mockEmailService;

    @Value("${app.notification.enabled:false}")
    private boolean notificationEnabled;

    public FoodController(FoodRepository foodRepository, UserRepository userRepository, SavedRecipeRepository savedRecipeRepository) {
        this.foodRepository = foodRepository;
        this.userRepository = userRepository;
        this.savedRecipeRepository = savedRecipeRepository;
    }

    private User getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        // メールアドレスで検索する
        return userRepository.findByEmail(auth.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    @GetMapping
    public String index(Model model) {
        User currentUser = getCurrentUser();
        
        // ▼▼▼ これが不足していたためエラーになっていました！ ▼▼▼
        model.addAttribute("user", currentUser);

        LocalDate now = LocalDate.now();
        LocalDate threeDaysLater = now.plusDays(3);

        var foods = foodRepository.findByUserOrderByExpirationDateAsc(currentUser);
        var warning = foodRepository.findByUserAndExpirationDateBetween(currentUser, now, threeDaysLater);
        var expired = foodRepository.findByUserAndExpirationDateBefore(currentUser, now);

        var savedRecipes = savedRecipeRepository.findByUserOrderBySavedAtDesc(currentUser);
        model.addAttribute("savedRecipes", savedRecipes);

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
        LocalDate expDate = LocalDate.parse(expirationDate);

        // ▼▼▼ 追加: 過去の日付チェック（今日より前ならエラー） ▼▼▼
        if (expDate.isBefore(LocalDate.now())) {
            return "redirect:/add?error=past_date";
        }

        Food food = new Food();
        food.setName(name);
        food.setExpirationDate(expDate);
        food.setUser(currentUser);

        foodRepository.save(food);
        checkAndSendImmediateNotification(food, currentUser);

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

        LocalDate newDate = LocalDate.parse(expirationDate);
        
        if (!food.getExpirationDate().equals(newDate)) {
            food.setNotificationSent(false);
        }

        food.setName(name);
        food.setExpirationDate(newDate);

        checkAndSendImmediateNotification(food, currentUser);

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

    private void checkAndSendImmediateNotification(Food food, User user) {
        if (food.getExpirationDate() == null) {
            return;
        }

        long daysUntil = ChronoUnit.DAYS.between(LocalDate.now(), food.getExpirationDate());

        if (daysUntil <= user.getNotificationDaysBefore() && daysUntil >= 0) {
            try {
                if (emailService != null) {
                    emailService.sendExpirationNotification(food);
                } else if (mockEmailService != null) {
                    mockEmailService.sendExpirationNotification(food);
                }
            } catch (Exception e) {
                log.error("即時通知の送信に失敗しました", e);
            }
        }
    }
}