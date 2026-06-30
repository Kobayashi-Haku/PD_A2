package com.example.foodmanager.repository;

import com.example.foodmanager.model.NotificationLog;
import com.example.foodmanager.model.User;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NotificationLogRepository extends JpaRepository<NotificationLog, Long> {
    List<NotificationLog> findByUserOrderBySentAtDesc(User user, Pageable pageable);
}
