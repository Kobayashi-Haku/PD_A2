package com.example.foodmanager.controller;

import com.example.foodmanager.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

// 追加: 自動ログインのために必要
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
                             HttpServletRequest request) { // 追加: リクエスト情報を受け取る
        try {
            // 1. ユーザー登録を実行
            userService.registerUser(username, email, password);
            
            // 2. 追加: そのまま自動でログイン処理を行う
            try {
                request.login(username, password);
            } catch (ServletException e) {
                // 万が一自動ログインに失敗した場合はログイン画面へ
                return "redirect:/login?error";
            }

            // 3. 変更: ログイン画面ではなく、トップページへ直接リダイレクト
            return "redirect:/";

        } catch (RuntimeException e) {
            model.addAttribute("error", e.getMessage());
            return "register";
        }
    }
}