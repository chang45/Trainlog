package org.examplexxx.trainlog.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.examplexxx.trainlog.entity.SysUser;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 返回给前端的用户信息，注意不能包含 passwordHash。
 */
@Data
@AllArgsConstructor
public class UserVO {

    private Long id;
    private String username;
    private String nickname;
    private BigDecimal heightCm;
    private BigDecimal weightKg;
    private Integer age;
    private String gender;
    private Integer dailyCalories;
    private LocalDateTime lastLoginAt;
    private Integer loginCount;

    public static UserVO from(SysUser user) {
        return new UserVO(user.getId(), user.getUsername(), user.getNickname(),
                user.getHeightCm(), user.getWeightKg(),
                user.getAge(), user.getGender(), user.getDailyCalories(),
                user.getLastLoginAt(), user.getLoginCount());
    }
}
