package org.examplexxx.trainlog.dto;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 每日自动消耗热量推算入参。
 * 不传的字段用当前登录用户资料里的值。
 */
@Data
public class DailyCaloriesRequest {

    private BigDecimal heightCm;

    private BigDecimal weightKg;

    private Integer age;

    /** MALE / FEMALE */
    private String gender;
}
