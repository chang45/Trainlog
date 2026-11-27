package org.examplexxx.trainlog.vo;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 图片识别饮食返回。
 */
@Data
@AllArgsConstructor
public class DietRecognitionVO {

    private String foodName;

    private Integer calories;

    private BigDecimal protein;

    private BigDecimal carbs;

    private BigDecimal fat;

    private String explanation;
}
