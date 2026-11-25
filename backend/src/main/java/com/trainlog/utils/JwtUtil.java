package com.trainlog.utils;

import com.trainlog.constant.JwtClaimsConstant;
import com.trainlog.entity.SysUser;
import com.trainlog.properties.JwtProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;

@Component
public class JwtUtil {

    private final SecretKey key;
    private final long ttlSeconds;

    public JwtUtil(JwtProperties properties) {
        byte[] bytes = properties.getSecret().getBytes(StandardCharsets.UTF_8);
        if (bytes.length < 32) {
            throw new IllegalStateException("app.jwt.secret 至少需要 32 个字符");
        }
        this.key = Keys.hmacShaKeyFor(bytes);
        this.ttlSeconds = properties.getTtlSeconds();
    }

    public String createToken(SysUser user) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(user.getUsername())
                .claim(JwtClaimsConstant.UID, user.getId())
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusSeconds(ttlSeconds)))
                .signWith(key)
                .compact();
    }

    public Claims parseToken(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public long getTtlSeconds() {
        return ttlSeconds;
    }
}
