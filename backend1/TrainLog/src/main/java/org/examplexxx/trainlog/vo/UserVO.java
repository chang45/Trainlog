package org.examplexxx.trainlog.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.examplexxx.trainlog.entity.SysUser;
import org.examplexxx.trainlog.entity.UserNutritionTarget;

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
    private Integer proteinNormal;
    private Integer proteinBulk;
    private Integer proteinCut;
    private LocalDateTime lastLoginAt;
    private Integer loginCount;

    /** 没有营养目标记录时，三个营养字段为 null */
    public static UserVO from(SysUser user, UserNutritionTarget target) {
        return new UserVO(user.getId(), user.getUsername(), user.getNickname(),
                user.getHeightCm(), user.getWeightKg(),
                user.getAge(), user.getGender(),
                target == null ? null : target.getDailyCalories(),
                target == null ? null : target.getProteinNormal(),
                target == null ? null : target.getProteinBulk(),
                target == null ? null : target.getProteinCut(),
                user.getLastLoginAt(), user.getLoginCount());
    }

    public static UserVO from(SysUser user) {
        return from(user, null);
    }
}
