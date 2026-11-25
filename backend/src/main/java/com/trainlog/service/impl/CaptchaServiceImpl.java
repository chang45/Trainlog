package com.trainlog.service.impl;

import com.trainlog.properties.CaptchaProperties;
import com.trainlog.service.CaptchaService;
import com.trainlog.utils.CaptchaUtil;
import com.trainlog.vo.CaptchaVO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
@Slf4j
public class CaptchaServiceImpl implements CaptchaService {

    private static final Logger log = LoggerFactory.getLogger(CaptchaServiceImpl.class);
    private static final String CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final int MAX_ENTRIES = 20000;

    private final SecureRandom random = new SecureRandom();
    private final Map<String, Entry> store = new ConcurrentHashMap<>();
    private final CaptchaProperties properties;
    private final Duration ttl;

    public CaptchaServiceImpl(CaptchaProperties properties) {
        this.properties = properties;
        this.ttl = Duration.ofSeconds(properties.getTtlSeconds());
    }

    @Override
    public CaptchaVO create() {
        if (store.size() > MAX_ENTRIES) {
            purgeExpired();
            if (store.size() > MAX_ENTRIES) {
                store.clear();
            }
        }
        String code = randomCode();
        String id = UUID.randomUUID().toString().replace("-", "");
        store.put(id, new Entry(code, Instant.now().plus(ttl)));
        if (properties.isLogCode()) {
            log.info("[验证码] captchaId={} code={}", id, code);
        }
        String image = CaptchaUtil.render(code, properties.getWidth(), properties.getHeight());
        return new CaptchaVO(id, image, (int) ttl.toSeconds());
    }

    @Override
    public boolean verify(String captchaId, String code) {
        if (captchaId == null || code == null) {
            return false;
        }
        Entry entry = store.remove(captchaId.trim());
        if (entry == null || Instant.now().isAfter(entry.expiresAt())) {
            return false;
        }
        return entry.code().equalsIgnoreCase(code.trim());
    }

    @Scheduled(fixedDelay = 60_000)
    public void purgeExpired() {
        Instant now = Instant.now();
        store.entrySet().removeIf(e -> now.isAfter(e.getValue().expiresAt()));
    }

    private String randomCode() {
        StringBuilder sb = new StringBuilder(properties.getLength());
        for (int i = 0; i < properties.getLength(); i++) {
            sb.append(CHARS.charAt(random.nextInt(CHARS.length())));
        }
        return sb.toString();
    }

    private record Entry(String code, Instant expiresAt) {
    }
}
