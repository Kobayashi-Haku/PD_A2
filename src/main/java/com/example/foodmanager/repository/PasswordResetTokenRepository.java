package com.example.foodmanager.repository;

import com.example.foodmanager.model.PasswordResetToken;
import com.example.foodmanager.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Long> {
    Optional<PasswordResetToken> findByTokenHash(String tokenHash);
    void deleteByTokenHash(String tokenHash); // 使用後に削除するため
    Optional<PasswordResetToken> findByUser(User user);
}