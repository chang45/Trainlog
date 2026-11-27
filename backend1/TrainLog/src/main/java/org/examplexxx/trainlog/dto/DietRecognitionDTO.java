package org.examplexxx.trainlog.dto;

import lombok.Data;

/**
 * 图片识别饮食入参。
 */
@Data
public class DietRecognitionDTO {

    /** BREAKFAST / LUNCH / DINNER，仅用于提示词 */
    private String mealType;

    /** data:image/jpeg;base64,... */
    private String imageBase64;
}
