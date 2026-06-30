package com.example.foodmanager.service;

import com.example.foodmanager.model.SavedRecipe;
import com.example.foodmanager.model.User;
import com.example.foodmanager.repository.FoodRepository;
import com.example.foodmanager.repository.PasswordResetTokenRepository;
import com.example.foodmanager.repository.SavedRecipeRepository;
import com.example.foodmanager.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class UserService implements UserDetailsService {

    private static final Pattern HAS_LETTER = Pattern.compile(".*[A-Za-z].*");
    private static final Pattern HAS_DIGIT = Pattern.compile(".*[0-9].*");

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final FoodRepository foodRepository;
    private final SavedRecipeRepository savedRecipeRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        // 1. 入力されたメールアドレスでユーザーを探す
        User user = userRepository.findByEmail(email)
            .orElseThrow(() -> new UsernameNotFoundException("User not found with email: " + email));

        return org.springframework.security.core.userdetails.User
            // 2. ▼▼▼ 修正: ここを getEmail() から getUsername() に変更 ▼▼▼
            // これにより、画面表示や auth.getName() が「ユーザーネーム」になります
            .withUsername(user.getEmail())
            .password(user.getPassword())
            .roles(user.getRole().replace("ROLE_", ""))
            .build();
    }

    /**
     * パスワードの強度を検証します。
     * 6文字以上、かつ英字と数字の両方を含む必要があります。
     */
    public static boolean isPasswordStrongEnough(String password) {
        if (password == null || password.length() < 6) {
            return false;
        }
        return HAS_LETTER.matcher(password).matches() && HAS_DIGIT.matcher(password).matches();
    }

    public static final String PASSWORD_REQUIREMENT_MESSAGE =
            "パスワードは6文字以上で、英字と数字の両方を含めてください。";

    @Transactional
    public User registerUser(String username, String email, String password) {

        if (userRepository.findByEmail(email).isPresent()) {
            throw new RuntimeException("このメールアドレスは既に登録されています。");
        }

        if (!isPasswordStrongEnough(password)) {
            throw new RuntimeException(PASSWORD_REQUIREMENT_MESSAGE);
        }

        User user = new User();
        user.setUsername(username);
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(password));
        return userRepository.save(user);
    }

    /**
     * アカウントとそれに紐づくデータ（食品・保存レシピ・パスワードリセットトークン）を削除します。
     */
    @Transactional
    public void deleteAccount(User user) {
        foodRepository.deleteAll(foodRepository.findByUser(user));
        savedRecipeRepository.deleteAll(savedRecipeRepository.findByUserOrderBySavedAtDesc(user));
        passwordResetTokenRepository.findByUser(user).ifPresent(passwordResetTokenRepository::delete);
        userRepository.delete(user);
    }
}
