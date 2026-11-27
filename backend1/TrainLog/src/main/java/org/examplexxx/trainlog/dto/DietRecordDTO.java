package org.examplexxx.trainlog.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 新增饮食记录入参。
 */
@Data
public class DietRecordDTO {

    /** BREAKFAST / LUNCH / DINNER */
    private String mealType;

    private String foodName;

    private Integer calories;

    private BigDecimal protein;

    private BigDecimal carbs;

    private BigDecimal fat;

    private LocalDate eatenDate;
}
