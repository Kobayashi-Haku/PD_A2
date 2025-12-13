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

    // コンストラクタ
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

        // 消費期限が近い順に並び替えて取得
        var foods = foodRepository.findByUserOrderByExpirationDateAsc(currentUser);
        var warning = foodRepository.findByUserAndExpirationDateBetween(currentUser, now, threeDaysLater);
        var expired = foodRepository.findByUserAndExpirationDateBefore(currentUser, now);

        // 保存したレシピを取得
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
        Food food = new Food();
        food.setName(name);
        food.setExpirationDate(LocalDate.parse(expirationDate));
        food.setUser(currentUser);

        // 保存
        foodRepository.save(food);

        // ▼▼▼ 修正箇所: currentUser を渡すように変更 ▼▼▼
        checkAndSendImmediateNotification(food, currentUser);

        return "redirect:/";
    }

    // 編集画面表示
    @GetMapping("/edit/{id}")
    public String editForm(@PathVariable Long id, Model model) {
        User currentUser = getCurrentUser();
        Food food = foodRepository.findById(id)
                .filter(f -> f.getUser().equals(currentUser))
                .orElseThrow(() -> new RuntimeException("Food not found or access denied"));

        model.addAttribute("food", food);
        return "edit";
    }

    // 更新処理
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

        if (!food.getExpirationDate().equals(newDate)) {
            food.setNotificationSent(false);
        }

        food.setName(name);
        food.setExpirationDate(newDate);

        // 更新時も、もし期限が近ければ通知を送るかチェック（必要なら）
        checkAndSendImmediateNotification(food, currentUser);

        foodRepository.save(food);
        return "redirect:/";
    }

    // 削除処理
    @PostMapping("/delete")
    public String delete(@RequestParam Long id) {
        User currentUser = getCurrentUser();
        foodRepository.findById(id)
                .filter(food -> food.getUser().equals(currentUser))
                .ifPresent(foodRepository::delete);
        return "redirect:/";
    }

    // ▼▼▼ 即時通知チェック用メソッド ▼▼▼
    private void checkAndSendImmediateNotification(Food food, User user) {
        // 消費期限が設定されていない場合は何もしない
        if (food.getExpirationDate() == null) {
            return;
        }

        // 「今日」から「消費期限」までの日数を計算
        long daysUntil = ChronoUnit.DAYS.between(LocalDate.now(), food.getExpirationDate());

        // ロジック:
        // 「残り日数」が「通知設定の日数」以下であれば、すでに危険域なので通知する。
        // かつ、期限切れ（マイナス）でない場合に送る
        if (daysUntil <= user.getNotificationDaysBefore() && daysUntil >= 0) {
            try {
                if (emailService != null) {
                    emailService.sendExpirationNotification(food);
                    System.out.println("即時通知メールを送信しました: " + food.getName());
                } else if (mockEmailService != null) {
                    // ローカル開発用（モック）の場合も動くように対応
                    mockEmailService.sendExpirationNotification(food);
                }
            } catch (Exception e) {
                log.error("即時通知の送信に失敗しました", e);
            }
        }
    }
}