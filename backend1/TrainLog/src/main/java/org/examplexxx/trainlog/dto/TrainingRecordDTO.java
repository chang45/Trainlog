package org.examplexxx.trainlog.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 新增 / 修改训练记录的入参。
 * 项目没有引入 spring-boot-starter-validation，字段校验放在 Service 里手动做。
 */
@Data
public class TrainingRecordDTO {

    /** 训练项目，例如：卧推、5 公里慢跑 */
    private String title;

    /** 训练大类：STRENGTH / CARDIO */
    private String category;

    /** 具体项目：CHEST/SHOULDER/BACK/LEG/INCLINE/CYCLING/RUNNING */
    private String type;

    /** 训练日期，格式 yyyy-MM-dd */
    private LocalDate trainingDate;

    /** 时长（分钟） */
    private Integer durationMinutes;

    /** 有氧：距离（公里） */
    private BigDecimal distanceKm;

    /** 力量：组数 */
    private Integer sets;

    /** 力量：每组次数 */
    private Integer reps;

    /** 力量：重量（公斤） */
    private BigDecimal weightKg;

    /** 消耗热量（千卡） */
    private Integer calories;

    /** 主观强度 RPE 1-10 */
    private Integer intensity;

    /** 备注 */
    private String notes;
}
