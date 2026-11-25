package com.trainlog.config;

import com.trainlog.constant.TrainingType;
import com.trainlog.entity.SysUser;
import com.trainlog.entity.TrainingRecord;
import com.trainlog.mapper.TrainingRecordMapper;
import com.trainlog.mapper.UserMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 首次启动时写入演示账号和几条训练记录，已有数据则不动。
 */
@Component
public class DataInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private final UserMapper userMapper;
    private final TrainingRecordMapper trainingRecordMapper;
    private final PasswordEncoder passwordEncoder;
    private final String username;
    private final String password;
    private final String nickname;

    public DataInitializer(UserMapper userMapper,
                           TrainingRecordMapper trainingRecordMapper,
                           PasswordEncoder passwordEncoder,
                           @Value("${app.seed.username:admin}") String username,
                           @Value("${app.seed.password:123456}") String password,
                           @Value("${app.seed.nickname:训练者}") String nickname) {
        this.userMapper = userMapper;
        this.trainingRecordMapper = trainingRecordMapper;
        this.passwordEncoder = passwordEncoder;
        this.username = username;
        this.password = password;
        this.nickname = nickname;
    }

    @Override
    public void run(ApplicationArguments args) {
        SysUser user = userMapper.findByUsername(username).orElseGet(this::createDemoUser);
        if (user != null && trainingRecordMapper.countByUserId(user.getId()) == 0) {
            seedRecords(user.getId());
        }
    }

    private SysUser createDemoUser() {
        if (userMapper.count() > 0) {
            return null;
        }
        SysUser user = new SysUser();
        user.setUsername(username);
        user.setPasswordHash(passwordEncoder.encode(password));
        user.setNickname(nickname);
        user.setEnabled(true);
        user.setCreatedAt(LocalDateTime.now());
        user.setLoginCount(0);
        userMapper.save(user);
        log.info("已初始化演示账号：{} / {}（上线前请务必修改）", username, password);
        return user;
    }

    private void seedRecords(Long userId) {
        LocalDate today = LocalDate.now();
        add(userId, "晨间拉伸", TrainingType.STRETCHING, today, 15, null, 45, 3, "唤醒身体，重点放松髋部");
        add(userId, "5 公里慢跑", TrainingType.RUNNING, today.minusDays(1), 32, 5.0, 310, 6, "配速 6'24\"，状态不错");
        add(userId, "上肢力量", TrainingType.STRENGTH, today.minusDays(2), 55, null, 380, 8, "卧推、划船、引体各 4 组");
        add(userId, "动感单车", TrainingType.CYCLING, today.minusDays(4), 45, 18.5, 420, 7, "间歇骑行 8 组");
        add(userId, "核心训练", TrainingType.STRENGTH, today.minusDays(6), 35, null, 210, 6, "平板支撑、悬垂举腿");
        add(userId, "自由泳", TrainingType.SWIMMING, today.minusDays(8), 40, 1.2, 360, 6, "1000 米，主要是技术练习");
        add(userId, "瑜伽伸展", TrainingType.STRETCHING, today.minusDays(9), 30, null, 120, 3, "睡前放松");
        add(userId, "HIIT 燃脂", TrainingType.HIIT, today.minusDays(11), 25, null, 300, 9, "波比跳 + 壶铃摆");
    }

    private void add(Long userId, String title, TrainingType type, LocalDate date, int minutes,
                     Double distance, Integer calories, Integer intensity, String notes) {
        TrainingRecord record = new TrainingRecord();
        record.setUserId(userId);
        record.setTitle(title);
        record.setType(type.name());
        record.setTrainingDate(date);
        record.setDurationMinutes(minutes);
        record.setDistanceKm(distance);
        record.setCalories(calories);
        record.setIntensity(intensity);
        record.setNotes(notes);
        record.setCreatedAt(LocalDateTime.now());
        record.setUpdatedAt(LocalDateTime.now());
        trainingRecordMapper.save(record);
    }
}
