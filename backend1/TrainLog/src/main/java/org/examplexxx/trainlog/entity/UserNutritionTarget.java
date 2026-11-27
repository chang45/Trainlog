package org.examplexxx.trainlog.entity;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户营养目标，对应表 user_nutrition_target。
 * user_id 外键指向 sys_user.id，一个用户一行。
 */
@Data
public class UserNutritionTarget {

    private Long id;

    /** 外键：sys_user.id */
    private Long userId;

    /** 每日总消耗（千卡） */
    private Integer dailyCalories;

    /** 正常维持蛋白质（克/天） */
    private Integer proteinNormal;

    /** 增肌蛋白质（克/天） */
    private Integer proteinBulk;

    /** 减脂蛋白质（克/天） */
    private Integer proteinCut;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
