package com.example.foodmanager.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Data
@NoArgsConstructor
public class PasswordResetToken {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * トークンの生の値はDBに保存しません。SHA-256でハッシュ化した値のみ保存します。
     * （URLが漏れてもDB内容からトークンを復元できないようにするため）
     */
    @Column(nullable = false, unique = true)
    private String tokenHash;

    @OneToOne(targetEntity = User.class, fetch = FetchType.EAGER)
    @JoinColumn(nullable = false, name = "user_id")
    private User user;

    private LocalDateTime expiryDate;

    public PasswordResetToken(String tokenHash, User user) {
        this.tokenHash = tokenHash;
        this.user = user;
        // トークンの有効期限は24時間に設定
        this.expiryDate = LocalDateTime.now().plusHours(24);
    }
}
