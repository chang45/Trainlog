package org.examplexxx.trainlog.config;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class PasswordConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        // Argon2id：salt 16B、hash 32B、并行度 1、内存 19456KiB(≈19MB)、迭代 2 次
        return new Argon2PasswordEncoder(16, 32, 1, 19456, 2);
    }
}