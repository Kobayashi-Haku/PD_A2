package com.example.foodmanager.controller;

import com.example.foodmanager.model.Food;
import com.example.foodmanager.model.User;
import com.example.foodmanager.repository.FoodRepository;
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
    
    @Autowired(required = false)
    private EmailService emailService;
    
    @Autowired(required = false)
    private MockEmailService mockEmailService;
    
    @Value("${app.notification.enabled:false}")
    private boolean notificationEnabled;

    public FoodController(FoodRepository foodRepository, UserRepository userRepository) {
        this.foodRepository = foodRepository;
        this.userRepository = userRepository;
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

        var foods = foodRepository.findByUser(currentUser);
        var warning = foodRepository.findByUserAndExpirationDateBetween(currentUser, now, threeDaysLater);
        var expired = foodRepository.findByUserAndExpirationDateBefore(currentUser, now);

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
        
        // 食品を保存
        Food savedFood = foodRepository.save(food);
        
        // 消費期限が明日の場合、即座に通知を送信
        LocalDate tomorrow = LocalDate.now().plusDays(1);
        if (savedFood.getExpirationDate().equals(tomorrow)) {
            log.info("消費期限が明日の食品が登録されました。即座通知を送信します - 食品: {}, ユーザー: {}", 
                    savedFood.getName(), currentUser.getUsername());
            
            try {
                // 適切なメールサービスを使用して即座通知
                if (notificationEnabled && emailService != null) {
                    emailService.sendImmediateExpirationNotification(savedFood);
                } else if (mockEmailService != null) {
                    mockEmailService.sendImmediateExpirationNotification(savedFood);
                }
                
                // 即座通知を送信したので、通知送信フラグをtrueに設定
                savedFood.setNotificationSent(true);
                foodRepository.save(savedFood);
                
            } catch (Exception e) {
                log.error("即座通知の送信に失敗しました - 食品: {}, ユーザー: {}", 
                         savedFood.getName(), currentUser.getUsername(), e);
            }
        }
        
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