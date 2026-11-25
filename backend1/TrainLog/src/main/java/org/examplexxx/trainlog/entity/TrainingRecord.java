package org.examplexxx.trainlog.entity;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 训练记录实体，对应数据库表 training_record
 */
@Data
public class TrainingRecord {

    /** 主键 */
    private Long id;

    /** 所属用户，对应 sys_user.id */
    private Long userId;

    /** 训练项目，例如：卧推、5 公里慢跑 */
    private String title;

    /** 训练大类：STRENGTH-力量训练 / CARDIO-有氧训练 */
    private String category;

    /** 具体项目：CHEST/SHOULDER/BACK/LEG/INCLINE/CYCLING/RUNNING */
    private String type;

    /** 训练日期 */
    private LocalDate trainingDate;

    /** 时长（分钟） */
    private Integer durationMinutes;

    /** 有氧：距离（公里），力量训练为 null */
    private BigDecimal distanceKm;

    /** 力量：组数 */
    private Integer sets;

    /** 力量：每组次数 */
    private Integer reps;

    /** 力量：重量（公斤），自重动作为 null */
    private BigDecimal weightKg;

    /** 消耗热量（千卡） */
    private Integer calories;

    /** 主观强度 RPE 1-10 */
    private Integer intensity;

    /** 备注 */
    private String notes;

    /** 创建时间 */
    private LocalDateTime createdAt;

    /** 更新时间 */
    private LocalDateTime updatedAt;
}
