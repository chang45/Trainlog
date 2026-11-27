package org.examplexxx.trainlog.entity;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 每日饮食记录，对应表 diet_record。
 */
@Data
public class DietRecord {

    private Long id;

    private Long userId;

    /** BREAKFAST / LUNCH / DINNER */
    private String mealType;

    private String foodName;

    private Integer calories;

    private BigDecimal protein;

    private BigDecimal carbs;

    private BigDecimal fat;

    private LocalDate eatenDate;

    private LocalDateTime createdAt;
}
