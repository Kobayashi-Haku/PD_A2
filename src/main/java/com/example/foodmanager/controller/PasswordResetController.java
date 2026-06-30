package com.example.foodmanager.controller;

import com.example.foodmanager.model.PasswordResetToken;
import com.example.foodmanager.model.User;
import com.example.foodmanager.repository.PasswordResetTokenRepository;
import com.example.foodmanager.repository.UserRepository;
import com.example.foodmanager.service.EmailService;
import com.example.foodmanager.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.HexFormat;
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
            // トークン生成（生の値はメールでのみ送信し、DBにはハッシュ値だけ保存する）
            String rawToken = UUID.randomUUID().toString();
            String tokenHash = hashToken(rawToken);
            PasswordResetToken myToken = new PasswordResetToken(tokenHash, user);
            tokenRepository.save(myToken);

            // リセット用URLの作成（現在のドメインを自動取得）
            String baseUrl = ServletUriComponentsBuilder.fromRequestUri(request)
                    .replacePath(null)
                    .build()
                    .toUriString();
            String resetUrl = baseUrl + "/reset-password?token=" + rawToken;

            // メール送信
            emailService.sendPasswordResetEmail(user.getEmail(), resetUrl);
        }

        // セキュリティのため、登録があってもなくても「送信しました」と表示
        return "redirect:/forgot-password?sent";
    }

    // 3. パスワード再設定画面を表示
    @GetMapping("/reset-password")
    public String showResetPasswordForm(@RequestParam String token, Model model) {
        PasswordResetToken resetToken = tokenRepository.findByTokenHash(hashToken(token)).orElse(null);

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
        String tokenHash = hashToken(token);
        PasswordResetToken resetToken = tokenRepository.findByTokenHash(tokenHash).orElse(null);

        if (resetToken == null || resetToken.getExpiryDate().isBefore(LocalDateTime.now())) {
            return "redirect:/login?error";
        }

        if (!UserService.isPasswordStrongEnough(password)) {
            model.addAttribute("token", token);
            model.addAttribute("error", UserService.PASSWORD_REQUIREMENT_MESSAGE);
            return "reset-password";
        }

        // パスワード更新
        User user = resetToken.getUser();
        user.setPassword(passwordEncoder.encode(password));
        userRepository.save(user);

        // 使用済みトークンを削除
        tokenRepository.deleteByTokenHash(tokenHash);

        return "redirect:/login?resetSuccess";
    }

    private String hashToken(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(rawToken.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hashBytes);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }
}
