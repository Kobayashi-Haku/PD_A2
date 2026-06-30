package com.example.foodmanager.config;

import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 簡易的なログイン試行回数の制限（同一IPからの総当たり攻撃対策）。
 * 1分間に5回失敗すると、しばらくの間そのIPからのログインをブロックします。
 */
@Component
public class LoginRateLimiter {

    private static final int MAX_ATTEMPTS = 5;
    private static final long WINDOW_MILLIS = 60_000L; // 1分
    private static final long COOLDOWN_MILLIS = 5 * 60_000L; // 5分のクールダウン

    private static class Attempts {
        int count;
        long windowStart;
        long blockedUntil;
    }

    private final ConcurrentHashMap<String, Attempts> attemptsByIp = new ConcurrentHashMap<>();

    public boolean isBlocked(String ip) {
        Attempts a = attemptsByIp.get(ip);
        if (a == null) {
            return false;
        }
        synchronized (a) {
            return a.blockedUntil > System.currentTimeMillis();
        }
    }

    public void recordFailure(String ip) {
        Attempts a = attemptsByIp.computeIfAbsent(ip, k -> new Attempts());
        synchronized (a) {
            long now = System.currentTimeMillis();
            if (now - a.windowStart > WINDOW_MILLIS) {
                a.windowStart = now;
                a.count = 0;
            }
            a.count++;
            if (a.count >= MAX_ATTEMPTS) {
                a.blockedUntil = now + COOLDOWN_MILLIS;
            }
        }
    }

    public void recordSuccess(String ip) {
        attemptsByIp.remove(ip);
    }

    public long remainingCooldownSeconds(String ip) {
        Attempts a = attemptsByIp.get(ip);
        if (a == null) {
            return 0;
        }
        synchronized (a) {
            long remainMillis = a.blockedUntil - System.currentTimeMillis();
            return Math.max(0, remainMillis / 1000);
        }
    }

    // 未使用だが将来のデバッグ・監視用に残しておく
    public Instant lastWindowStart(String ip) {
        Attempts a = attemptsByIp.get(ip);
        return a == null ? null : Instant.ofEpochMilli(a.windowStart);
    }
}
