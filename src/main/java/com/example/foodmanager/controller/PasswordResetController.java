package com.example.foodmanager.controller;

import com.example.foodmanager.model.PasswordResetToken;
import com.example.foodmanager.model.User;
import com.example.foodmanager.repository.PasswordResetTokenRepository;
import com.example.foodmanager.repository.UserRepository;
import com.example.foodmanager.service.EmailService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.time.LocalDateTime;
import java.util.UUID;

@Controller
@RequiredArgsConstructor
public class PasswordResetController {

    private final UserRepository userRepository;
    private final PasswordResetTokenRepository tokenRepository;
    private final EmailService emailService;
    private final PasswordEncoder passwordEncoder;

    // 1. メールアドレス入力画面を表示
    @GetMapping("/forgot-password")
    public String showForgotPasswordForm() {
        return "forgot-password";
    }

    // 2. メール送信処理
    @PostMapping("/forgot-password")
    public String processForgotPassword(@RequestParam String email, HttpServletRequest request, Model model) {
        User user = userRepository.findByEmail(email).orElse(null);
        
        if (user != null) {
            // トークン生成
            String token = UUID.randomUUID().toString();
            PasswordResetToken myToken = new PasswordResetToken(token, user);
            tokenRepository.save(myToken);

            // リセット用URLの作成（現在のドメインを自動取得）
            String baseUrl = ServletUriComponentsBuilder.fromRequestUri(request)
                    .replacePath(null)
                    .build()
                    .toUriString();
            String resetUrl = baseUrl + "/reset-password?token=" + token;

            // メール送信
            emailService.sendPasswordResetEmail(user.getEmail(), resetUrl);
        }
        
        // セキュリティのため、登録があってもなくても「送信しました」と表示
        return "redirect:/forgot-password?sent";
    }

    // 3. パスワード再設定画面を表示
    @GetMapping("/reset-password")
    public String showResetPasswordForm(@RequestParam String token, Model model) {
        PasswordResetToken resetToken = tokenRepository.findByToken(token).orElse(null);

        if (resetToken == null || resetToken.getExpiryDate().isBefore(LocalDateTime.now())) {
            model.addAttribute("error", "リンクが無効か、期限切れです。");
            return "login"; // ログイン画面へ戻す
        }

        model.addAttribute("token", token);
        return "reset-password";
    }

    // 4. 新しいパスワードを保存
    @PostMapping("/reset-password")
    @Transactional
    public String processResetPassword(@RequestParam String token, @RequestParam String password, Model model) {
        PasswordResetToken resetToken = tokenRepository.findByToken(token).orElse(null);

        if (resetToken == null || resetToken.getExpiryDate().isBefore(LocalDateTime.now())) {
            return "redirect:/login?error";
        }

        // パスワード更新
        User user = resetToken.getUser();
        user.setPassword(passwordEncoder.encode(password));
        userRepository.save(user);

        // 使用済みトークンを削除
        tokenRepository.deleteByToken(token);

        return "redirect:/login?resetSuccess";
    }
}