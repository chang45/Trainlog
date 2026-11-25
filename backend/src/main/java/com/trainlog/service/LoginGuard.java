package com.trainlog.service;

import com.trainlog.exception.BizException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 登录 / 注册防护：IP 维度的请求频率限制、注册额度、用户名维度的失败次数锁定。
 * 单机内存实现，多实例部署时换成 Redis 即可。
 */
@Service
public class LoginGuard {

    private static final long RATE_WINDOW_MS = 60_000L;
    private static final long REGISTER_WINDOW_MS = 3_600_000L;

    private final Map<String, Window> windows = new ConcurrentHashMap<>();
    private final Map<String, Window> registrations = new ConcurrentHashMap<>();
    private final Map<String, Attempt> attempts = new ConcurrentHashMap<>();

    private final int captchaPerMinute;
    private final int loginPerMinute;
    private final int registerPerMinute;
    private final int registerPerHour;
    private final int maxFailures;
    private final long failureWindowMs;
    private final long lockMs;

    public LoginGuard(@Value("${app.security.captcha-per-minute:20}") int captchaPerMinute,
                      @Value("${app.security.login-per-minute:10}") int loginPerMinute,
                      @Value("${app.security.register-per-minute:5}") int registerPerMinute,
                      @Value("${app.security.register-per-hour:3}") int registerPerHour,
                      @Value("${app.security.max-failures:5}") int maxFailures,
                      @Value("${app.security.failure-window-seconds:600}") long failureWindowSeconds,
                      @Value("${app.security.lock-seconds:600}") long lockSeconds) {
        this.captchaPerMinute = captchaPerMinute;
        this.loginPerMinute = loginPerMinute;
        this.registerPerMinute = registerPerMinute;
        this.registerPerHour = registerPerHour;
        this.maxFailures = maxFailures;
        this.failureWindowMs = failureWindowSeconds * 1000L;
        this.lockMs = lockSeconds * 1000L;
    }

    public void checkCaptchaRate(String ip) {
        checkRate("captcha:" + ip, captchaPerMinute, RATE_WINDOW_MS, "刷新验证码太频繁了，请稍后再试");
    }

    public void checkLoginRate(String ip) {
        checkRate("login:" + ip, loginPerMinute, RATE_WINDOW_MS, "登录请求太频繁了，请稍后再试");
    }

    public void checkRegisterRate(String ip) {
        checkRate("register:" + ip, registerPerMinute, RATE_WINDOW_MS, "注册请求太频繁了，请稍后再试");
    }

    public void assertRegisterQuota(String ip) {
        Window window = registrations.get(registerKey(ip));
        if (window == null) {
            return;
        }
        long now = System.currentTimeMillis();
        synchronized (window) {
            if (!window.isExpired(now) && window.count >= registerPerHour) {
                throw new BizException(429, "该网络注册账号次数已达上限，请稍后再试");
            }
        }
    }

    public void recordRegistration(String ip) {
        long now = System.currentTimeMillis();
        Window window = registrations.computeIfAbsent(registerKey(ip),
                key -> new Window(now, REGISTER_WINDOW_MS));
        synchronized (window) {
            if (window.isExpired(now)) {
                window.reset(now);
            }
            window.count++;
        }
    }

    public void assertNotLocked(String username) {
        Attempt attempt = attempts.get(normalize(username));
        if (attempt == null) {
            return;
        }
        synchronized (attempt) {
            long now = System.currentTimeMillis();
            if (attempt.lockedUntil > now) {
                long minutes = Math.max(1, (attempt.lockedUntil - now + 59_999) / 60_000);
                throw new BizException(429, "密码错误次数过多，请 " + minutes + " 分钟后再试");
            }
        }
    }

    /**
     * @return 剩余可尝试次数，0 表示本次已触发锁定
     */
    public int registerFailure(String username) {
        Attempt attempt = attempts.computeIfAbsent(normalize(username), key -> new Attempt());
        synchronized (attempt) {
            long now = System.currentTimeMillis();
            if (now - attempt.firstFailureAt > failureWindowMs) {
                attempt.count = 0;
                attempt.firstFailureAt = now;
            }
            attempt.count++;
            if (attempt.count >= maxFailures) {
                attempt.lockedUntil = now + lockMs;
                attempt.count = 0;
                attempt.firstFailureAt = now;
                return 0;
            }
            return maxFailures - attempt.count;
        }
    }

    public void onSuccess(String username) {
        attempts.remove(normalize(username));
    }

    @Scheduled(fixedDelay = 120_000)
    public void purge() {
        long now = System.currentTimeMillis();
        windows.entrySet().removeIf(e -> e.getValue().isExpired(now));
        registrations.entrySet().removeIf(e -> e.getValue().isExpired(now));
        attempts.entrySet().removeIf(e -> {
            Attempt attempt = e.getValue();
            synchronized (attempt) {
                return attempt.lockedUntil < now && now - attempt.firstFailureAt > failureWindowMs;
            }
        });
    }

    private void checkRate(String key, int limit, long windowMs, String message) {
        long now = System.currentTimeMillis();
        Window window = windows.computeIfAbsent(key, k -> new Window(now, windowMs));
        synchronized (window) {
            if (window.isExpired(now)) {
                window.reset(now);
            }
            window.count++;
            if (window.count > limit) {
                throw new BizException(429, message);
            }
        }
    }

    private String registerKey(String ip) {
        return "registration:" + (ip == null ? "unknown" : ip);
    }

    private String normalize(String username) {
        return username == null ? "" : username.trim().toLowerCase(Locale.ROOT);
    }

    private static final class Window {
        private long start;
        private int count;
        private final long durationMs;

        private Window(long start, long durationMs) {
            this.start = start;
            this.durationMs = durationMs;
        }

        private boolean isExpired(long now) {
            return now - start >= durationMs;
        }

        private void reset(long now) {
            start = now;
            count = 0;
        }
    }

    private static final class Attempt {
        private long firstFailureAt = System.currentTimeMillis();
        private int count;
        private long lockedUntil;
    }
}
