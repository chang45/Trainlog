package org.examplexxx.trainlog.config;

import lombok.extern.slf4j.Slf4j;
import org.examplexxx.trainlog.entity.SysUser;
import org.examplexxx.trainlog.mapper.AuthMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * 演示账号初始化：表里没有该账号时按配置创建一次，密码用 Argon2id 加密后入库。
 * 账号密码来自 application-dev.yml 的 trainlog.user.* 配置。
 */
@Component
@Slf4j
public class AdminSeeder implements ApplicationRunner {

    private final AuthMapper authMapper;
    private final PasswordEncoder passwordEncoder;
    private final String username;
    private final String password;
    private final String nickname;

    public AdminSeeder(AuthMapper authMapper,
                       PasswordEncoder passwordEncoder,
                       @Value("${trainlog.user.username:admin}") String username,
                       @Value("${trainlog.user.password:123456}") String password,
                       @Value("${trainlog.user.nickname:训练者}") String nickname) {
        this.authMapper = authMapper;
        this.passwordEncoder = passwordEncoder;
        this.username = username;
        this.password = password;
        this.nickname = nickname;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (authMapper.findByUsername(username) != null) {
            return;
        }
        SysUser user = new SysUser();
        user.setUsername(username);
        user.setPasswordHash(passwordEncoder.encode(password));
        user.setNickname(nickname);
        user.setEnabled(1);
        user.setCreatedAt(LocalDateTime.now());
        user.setLoginCount(0);
        authMapper.insert(user);
        log.info("已初始化演示账号：{} / {}（上线前请务必修改）", username, password);
    }
}
