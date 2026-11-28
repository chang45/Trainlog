package org.examplexxx.trainlog.dto;

import lombok.Data;

import java.time.LocalDate;

/**
 * 图片识别饮食入参。
 */
@Data
public class DietRecognitionDTO {

    /** BREAKFAST / LUNCH / DINNER，仅用于提示词 */
    private String mealType;

    /** data:image/jpeg;base64,... */
    private String imageBase64;

    /** 记录到哪一天，不传默认今天 */
    private LocalDate eatenDate;
}
