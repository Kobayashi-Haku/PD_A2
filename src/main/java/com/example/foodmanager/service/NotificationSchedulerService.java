package com.example.foodmanager.service;

import com.example.foodmanager.model.Food;
import com.example.foodmanager.repository.FoodRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@Slf4j
public class NotificationSchedulerService {

    private final FoodRepository foodRepository;
    
    @Autowired(required = false)
    private EmailService emailService;
    
    @Autowired(required = false)
    private MockEmailService mockEmailService;
    
    @Value("${app.notification.enabled:false}")
    private boolean notificationEnabled;

    public NotificationSchedulerService(FoodRepository foodRepository) {
        this.foodRepository = foodRepository;
    }

    /**
     * 毎日午前9時に実行される通知チェック
     * 食品の登録から1日経過した食品の通知を送信
     */
    @Scheduled(cron = "0 0 9 * * *") // 毎日9:00AM
    @Transactional
    public void checkAndSendNotifications() {
        log.info("通知チェックを開始します（通知有効: {}）", notificationEnabled);
        
        try {
            // 登録から1日経過した食品で、まだ通知が送信されていないものを取得
            LocalDateTime oneDayAgo = LocalDateTime.now().minusDays(1);
            List<Food> foodsToNotify = foodRepository.findByRegisteredAtBeforeAndNotificationSentFalse(oneDayAgo);
            
            log.info("通知対象の食品数: {}", foodsToNotify.size());
            
            for (Food food : foodsToNotify) {
                try {
                    // 適切なメールサービスを使用
                    if (notificationEnabled && emailService != null) {
                        emailService.sendExpirationNotification(food);
                    } else if (mockEmailService != null) {
                        mockEmailService.sendExpirationNotification(food);
                    } else {
                        log.warn("メールサービスが利用できません");
                        continue;
                    }
                    
                    // 通知送信フラグを更新
                    food.setNotificationSent(true);
                    foodRepository.save(food);
                    
                    log.info("通知を送信しました - ユーザー: {}, 食品: {}", 
                            food.getUser().getUsername(), food.getName());
                            
                } catch (Exception e) {
                    log.error("個別通知の送信に失敗しました - ユーザー: {}, 食品: {}", 
                            food.getUser().getUsername(), food.getName(), e);
                }
            }
            
            log.info("通知チェックが完了しました。送信済み通知数: {}", foodsToNotify.size());
            
        } catch (Exception e) {
            log.error("通知チェック処理でエラーが発生しました", e);
        }
    }

    /**
     * テスト用：手動で通知チェックを実行
     */
    public void triggerNotificationCheck() {
        log.info("手動通知チェックが実行されました");
        checkAndSendNotifications();
    }
}