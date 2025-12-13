package com.example.foodmanager.controller;

import com.example.foodmanager.model.User;
import com.example.foodmanager.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalTime;

@Controller
public class SettingsController {

    @Autowired
    private UserRepository userRepository;

    private User getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        // ▼▼▼ 修正1: findByUsername ではなく findByEmail を使う ▼▼▼
        return userRepository.findByEmail(auth.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    @GetMapping("/settings")
    public String showSettings(Model model) {
        model.addAttribute("user", getCurrentUser());
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
    public String updateUsername(@RequestParam String newUsername) {
        User user = getCurrentUser();

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
}