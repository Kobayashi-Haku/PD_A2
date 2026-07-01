package com.example.foodmanager.controller;

import com.example.foodmanager.model.Food;
import com.example.foodmanager.model.FoodCategory;
import com.example.foodmanager.model.NotificationLog;
import com.example.foodmanager.model.User;
import com.example.foodmanager.repository.FoodRepository;
import com.example.foodmanager.repository.NotificationLogRepository;
import com.example.foodmanager.repository.SavedRecipeRepository;
import com.example.foodmanager.repository.UserRepository;
import com.example.foodmanager.service.EmailService;
import com.example.foodmanager.service.MockEmailService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Controller
@RequestMapping("/")
@Slf4j
public class FoodController {

    private final FoodRepository foodRepository;
    private final UserRepository userRepository;
    private final SavedRecipeRepository savedRecipeRepository;
    private final NotificationLogRepository notificationLogRepository;

    @Autowired(required = false)
    private EmailService emailService;

    @Autowired(required = false)
    private MockEmailService mockEmailService;

    @Value("${app.notification.enabled:false}")
    private boolean notificationEnabled;

    public FoodController(FoodRepository foodRepository, UserRepository userRepository,
                           SavedRecipeRepository savedRecipeRepository,
                           NotificationLogRepository notificationLogRepository) {
        this.foodRepository = foodRepository;
        this.userRepository = userRepository;
        this.savedRecipeRepository = savedRecipeRepository;
        this.notificationLogRepository = notificationLogRepository;
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

        // ダッシュボード用の集計
        model.addAttribute("totalCount", foods.size());
        model.addAttribute("expiringSoonCount", warning.size());
        model.addAttribute("expiredCount", expired.size());

        model.addAttribute("categories", FoodCategory.values());

        return "list";
    }

    @GetMapping("/add")
    public String addForm(Model model) {
        if (!model.containsAttribute("food")) {
            model.addAttribute("food", new Food());
        }
        model.addAttribute("categories", FoodCategory.values());
        model.addAttribute("today", java.time.LocalDate.now().toString());
        return "form";
    }

    @PostMapping("/add")
    public String addSubmit(@RequestParam String name,
                             @RequestParam String expirationDate,
                             @RequestParam(required = false) FoodCategory category,
                             Model model) {
        User currentUser = getCurrentUser();

        if (name == null || name.isBlank()) {
            return renderAddError(model, name, expirationDate, category, "食品名を入力してください。");
        }
        if (name.length() > 100) {
            return renderAddError(model, name, expirationDate, category, "食品名は100文字以内で入力してください。");
        }

        LocalDate expDate;
        try {
            expDate = LocalDate.parse(expirationDate);
        } catch (Exception e) {
            return renderAddError(model, name, expirationDate, category, "消費期限の形式が正しくありません。");
        }

        // ▼▼▼ 追加: 過去の日付チェック（今日より前ならエラー） ▼▼▼
        if (expDate.isBefore(LocalDate.now())) {
            return "redirect:/add?error=past_date";
        }

        Food food = new Food();
        food.setName(name);
        food.setExpirationDate(expDate);
        food.setCategory(category != null ? category : FoodCategory.OTHER);
        food.setUser(currentUser);

        foodRepository.save(food);
        checkAndSendImmediateNotification(food, currentUser);

        return "redirect:/?toast=added";
    }

    private String renderAddError(Model model, String name, String expirationDate, FoodCategory category, String error) {
        Food food = new Food();
        food.setName(name);
        if (category != null) {
            food.setCategory(category);
        }
        try {
            food.setExpirationDate(LocalDate.parse(expirationDate));
        } catch (Exception ignored) {
            // 入力値が不正な場合は未設定のままにする
        }
        model.addAttribute("food", food);
        model.addAttribute("categories", FoodCategory.values());
        model.addAttribute("error", error);
        return "form";
    }

    @GetMapping("/edit/{id}")
    public String editForm(@PathVariable Long id, Model model) {
        User currentUser = getCurrentUser();
        Food food = foodRepository.findById(id)
                .filter(f -> f.getUser().equals(currentUser))
                .orElseThrow(() -> new RuntimeException("Food not found or access denied"));

        model.addAttribute("food", food);
        model.addAttribute("categories", FoodCategory.values());
        return "edit";
    }

    @PostMapping("/update")
    public String update(@RequestParam Long id,
                         @RequestParam String name,
                         @RequestParam String expirationDate,
                         @RequestParam(required = false) FoodCategory category,
                         Model model) {
        User currentUser = getCurrentUser();
        Food food = foodRepository.findById(id)
                .filter(f -> f.getUser().equals(currentUser))
                .orElseThrow(() -> new RuntimeException("Food not found or access denied"));

        if (name == null || name.isBlank() || name.length() > 100) {
            model.addAttribute("food", food);
            model.addAttribute("categories", FoodCategory.values());
            model.addAttribute("error", name == null || name.isBlank()
                    ? "食品名を入力してください。" : "食品名は100文字以内で入力してください。");
            return "edit";
        }

        LocalDate newDate;
        try {
            newDate = LocalDate.parse(expirationDate);
        } catch (Exception e) {
            model.addAttribute("food", food);
            model.addAttribute("categories", FoodCategory.values());
            model.addAttribute("error", "消費期限の形式が正しくありません。");
            return "edit";
        }

        if (!food.getExpirationDate().equals(newDate)) {
            food.setNotificationSent(false);
        }

        food.setName(name);
        food.setExpirationDate(newDate);
        if (category != null) {
            food.setCategory(category);
        }

        checkAndSendImmediateNotification(food, currentUser);

        foodRepository.save(food);
        return "redirect:/?toast=updated";
    }

    @PostMapping("/delete")
    public String delete(@RequestParam Long id) {
        User currentUser = getCurrentUser();
        foodRepository.findById(id)
                .filter(food -> food.getUser().equals(currentUser))
                .ifPresent(foodRepository::delete);
        return "redirect:/?toast=deleted";
    }

    @PostMapping("/delete/bulk")
    public String deleteBulk(@RequestParam(required = false) List<Long> ids) {
        User currentUser = getCurrentUser();
        if (ids != null && !ids.isEmpty()) {
            List<Food> targets = foodRepository.findAllById(ids).stream()
                    .filter(f -> f.getUser().equals(currentUser))
                    .toList();
            foodRepository.deleteAll(targets);
        }
        return "redirect:/?toast=deleted";
    }

    @PostMapping("/delete/expired")
    public String deleteExpired() {
        User currentUser = getCurrentUser();
        List<Food> expired = foodRepository.findByUserAndExpirationDateBefore(currentUser, LocalDate.now());
        foodRepository.deleteAll(expired);
        return "redirect:/?toast=deleted";
    }

    @GetMapping("/export/csv")
    public void exportCsv(HttpServletResponse response) throws IOException {
        User currentUser = getCurrentUser();
        List<Food> foods = foodRepository.findByUserOrderByExpirationDateAsc(currentUser);

        response.setContentType("text/csv; charset=UTF-8");
        response.setCharacterEncoding("UTF-8");
        response.setHeader("Content-Disposition", "attachment; filename=\"foods.csv\"");

        // ExcelでBOM付きUTF-8を正しく文字化けせず開けるようにする
        response.getOutputStream().write(0xEF);
        response.getOutputStream().write(0xBB);
        response.getOutputStream().write(0xBF);

        try (PrintWriter writer = new PrintWriter(response.getOutputStream(), true, StandardCharsets.UTF_8)) {
            writer.println("名称,消費期限,カテゴリ,状態");
            LocalDate now = LocalDate.now();
            DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd");
            for (Food f : foods) {
                String status;
                if (f.getExpirationDate() == null) {
                    status = "未設定";
                } else {
                    long days = ChronoUnit.DAYS.between(now, f.getExpirationDate());
                    status = days < 0 ? "期限切れ" : (days <= 3 ? "間近" : "有効");
                }
                writer.println(String.join(",",
                        csvEscape(f.getName()),
                        f.getExpirationDate() != null ? f.getExpirationDate().format(fmt) : "",
                        csvEscape(f.getCategory() != null ? f.getCategory().getLabel() : ""),
                        csvEscape(status)
                ));
            }
        }
    }

    private String csvEscape(String value) {
        if (value == null) {
            return "";
        }
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
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
                notificationLogRepository.save(new NotificationLog(user, food.getName()));
            } catch (Exception e) {
                log.error("即時通知の送信に失敗しました", e);
            }
        }
    }
}
