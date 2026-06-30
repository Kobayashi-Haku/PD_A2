package com.example.foodmanager.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "foods")
public class Food {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "食品名を入力してください")
    @Size(max = 100, message = "食品名は100文字以内で入力してください")
    @Column(nullable = false)
    private String name;

    @NotNull(message = "消費期限を入力してください")
    @Column(nullable = false)
    private LocalDate expirationDate;

    @Column(nullable = false)
    private LocalDateTime registeredAt;

    @Column(nullable = false)
    private boolean notificationSent = false;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private FoodCategory category = FoodCategory.OTHER;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @PrePersist
    protected void onCreate() {
        registeredAt = LocalDateTime.now();
        if (category == null) {
            category = FoodCategory.OTHER;
        }
    }
}
