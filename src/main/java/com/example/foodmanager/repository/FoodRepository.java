package com.example.foodmanager.repository;

import com.example.foodmanager.model.Food;
import com.example.foodmanager.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public interface FoodRepository extends JpaRepository<Food, Long> {
    List<Food> findByUser(User user);
    List<Food> findByUserOrderByExpirationDateAsc(User user);
    List<Food> findByUserAndExpirationDateBefore(User user, LocalDate date);
    List<Food> findByUserAndExpirationDateBetween(User user, LocalDate start, LocalDate end);
    List<Food> findByRegisteredAtBeforeAndNotificationSentFalse(LocalDateTime registeredAt);
    List<Food> findByUserAndExpirationDate(User user, LocalDate date);
}