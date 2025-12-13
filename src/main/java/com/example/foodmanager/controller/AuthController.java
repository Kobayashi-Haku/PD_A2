package com.example.foodmanager.controller;

import com.example.foodmanager.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.ServletException;

@Controller
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;

    @GetMapping("/login")
    public String login() {
        return "login";
    }

    @GetMapping("/register")
    public String showRegistrationForm() {
        return "register";
    }

    @PostMapping("/register")
    public String registerUser(@RequestParam String username,
                             @RequestParam String email,
                             @RequestParam String password,
                             Model model,
                             HttpServletRequest request) {
        try {
            // 1. ユーザー登録を実行
            userService.registerUser(username, email, password);
            
            // 2. 自動ログイン処理
            try {
                // ▼▼▼ 修正: username ではなく email を使ってログインさせる ▼▼▼
                request.login(email, password);
            } catch (ServletException e) {
                return "redirect:/login?error";
            }

            return "redirect:/";

        } catch (RuntimeException e) {
            model.addAttribute("error", e.getMessage());
            return "register";
        }
    }
}