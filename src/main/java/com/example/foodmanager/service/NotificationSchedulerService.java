package com.example.foodmanager.service;

import com.example.foodmanager.model.Food;
import com.example.foodmanager.model.User;
import com.example.foodmanager.repository.FoodRepository;
import com.example.foodmanager.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationSchedulerService {

    private final FoodRepository foodRepository;
    private final UserRepository userRepository; // 追加

    @Autowired(required = false)
    private EmailService emailService;

    @Autowired(required = false)
    private MockEmailService mockEmailService;

    @Value("${app.notification.enabled:false}")
    private boolean notificationEnabled;

    // ▼▼▼ 変更: 毎日9時ではなく、30分ごとに実行 (毎時0分と30分) ▼▼▼
    @Scheduled(cron = "0 0/30 * * * *")
    @Transactional
    public void checkAndSendNotifications() {
        // 現在時刻を「分」までで切り捨てて取得（秒は00にする）
        LocalTime now = LocalTime.now().truncatedTo(ChronoUnit.MINUTES);
        log.info("定期通知チェック開始: 時刻 {}", now);

        // 1. 今この時間に通知を希望しているユーザーを探す
        List<User> targetUsers = userRepository.findByNotificationTime(now);
        
        if (targetUsers.isEmpty()) {
            return; // 対象ユーザーがいなければ終了
        }

        log.info("通知対象ユーザー数: {}", targetUsers.size());

        for (User user : targetUsers) {
            try {
                // 2. そのユーザーの設定（何日前？）に合わせてターゲット日を計算
                // 例: 今日が12/1で「3日前通知」なら、期限が12/4の食品を探す
                LocalDate targetExpirationDate = LocalDate.now().plusDays(user.getNotificationDaysBefore());

                // 3. そのユーザーの、その日が期限の食品を取得
                List<Food> foodsToExpire = foodRepository.findByUserAndExpirationDate(user, targetExpirationDate);

                if (!foodsToExpire.isEmpty()) {
                    for (Food food : foodsToExpire) {
                        sendNotification(food);
                    }
                    log.info("ユーザー {} に {} 件の通知を送りました", user.getUsername(), foodsToExpire.size());
                }
            } catch (Exception e) {
                log.error("ユーザー {} の通知処理中にエラー", user.getUsername(), e);
            }
        }
    }

    private void sendNotification(Food food) {
        try {
            if (notificationEnabled && emailService != null) {
                emailService.sendExpirationNotification(food);
            } else if (mockEmailService != null) {
                mockEmailService.sendExpirationNotification(food);
            }
        } catch (Exception e) {
            log.error("メール送信失敗: {}", food.getName(), e);
        }
    }
}