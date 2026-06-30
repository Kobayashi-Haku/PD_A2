package com.example.foodmanager.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

/**
 * 通知（消費期限メール）の送信履歴
 */
@Entity
@Data
@NoArgsConstructor
@Table(name = "notification_logs")
public class NotificationLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private String foodName;

    @Column(nullable = false)
    private LocalDateTime sentAt;

    public NotificationLog(User user, String foodName) {
        this.user = user;
        this.foodName = foodName;
        this.sentAt = LocalDateTime.now();
    }
}
