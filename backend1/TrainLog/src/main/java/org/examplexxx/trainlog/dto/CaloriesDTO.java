package org.examplexxx.trainlog.dto;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 热量估算入参：一次训练的信息。
 * 用户身高体重由后端从当前登录用户读，不需要前端传。
 */
@Data
public class CaloriesDTO {

    /** 具体项目：CHEST / SHOULDER / BACK / LEG / INCLINE / CYCLING / RUNNING */
    private String type;

    /** 时长（分钟） */
    private Integer durationMinutes;

    /** 距离（公里，有氧用） */
    private BigDecimal distanceKm;

    /** 组数（力量用） */
    private Integer sets;

    /** 每组次数（力量用） */
    private Integer reps;

    /** 负重（公斤，力量用，自重动作可不填） */
    private BigDecimal weightKg;

    /** 主观强度 RPE 1-10 */
    private Integer intensity;
}
