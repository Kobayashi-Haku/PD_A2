package com.example.foodmanager.controller;

import com.example.foodmanager.model.NotificationLog;
import com.example.foodmanager.model.User;
import com.example.foodmanager.repository.NotificationLogRepository;
import com.example.foodmanager.repository.UserRepository;
import com.example.foodmanager.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalTime;
import java.util.List;

@Controller
@RequiredArgsConstructor
public class SettingsController {

    private final UserRepository userRepository;
    private final UserService userService;
    private final NotificationLogRepository notificationLogRepository;

    private User getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        // ▼▼▼ 修正1: findByUsername ではなく findByEmail を使う ▼▼▼
        return userRepository.findByEmail(auth.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    @GetMapping("/settings")
    public String showSettings(Model model) {
        User user = getCurrentUser();
        model.addAttribute("user", user);

        List<NotificationLog> history = notificationLogRepository
                .findByUserOrderBySentAtDesc(user, PageRequest.of(0, 50));
        model.addAttribute("notificationHistory", history);

        return "settings";
    }

    @PostMapping("/settings")
    public String updateSettings(@RequestParam Integer days, @RequestParam String time) {
        User user = getCurrentUser();
        user.setNotificationDaysBefore(days);
        user.setNotificationTime(LocalTime.parse(time));
        userRepository.save(user);
        return "redirect:/settings?success";
    }

    @PostMapping("/settings/username")
    public String updateUsername(@RequestParam String newUsername, Model model) {
        User user = getCurrentUser();

        if (newUsername == null || newUsername.isBlank() || newUsername.length() < 3 || newUsername.length() > 20) {
            model.addAttribute("user", user);
            model.addAttribute("usernameError", "ユーザー名は3〜20文字で入力してください。");
            return "settings";
        }

        if (newUsername.equals(user.getUsername())) {
             return "redirect:/settings?tab=account";
        }

        // 重複チェックは削除済み（自由に設定可能）

        user.setUsername(newUsername);
        userRepository.save(user);

        // ▼▼▼ 修正2: セッション更新処理を削除しました ▼▼▼
        // ログインID（メールアドレス）は変わっていないので、何もしなくて大丈夫です。

        return "redirect:/settings?tab=account&success=username_updated";
    }

    @PostMapping("/settings/delete-account")
    public String deleteAccount(HttpServletRequest request) {
        User user = getCurrentUser();
        userService.deleteAccount(user);

        // ログアウトしてセッションを破棄
        try {
            request.logout();
        } catch (Exception ignored) {
            // 何もしない（アカウントはすでに削除済み）
        }

        return "redirect:/login?accountDeleted";
    }
}
