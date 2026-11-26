package org.examplexxx.trainlog.vo;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * 每日自动消耗热量推算结果。
 */
@Data
@AllArgsConstructor
public class DailyCaloriesVO {

    /** 基础代谢率（千卡/天） */
    private int bmr;

    /** 每日总消耗（千卡/天，含日常活动） */
    private int tdee;

    /** AI / FORMULA */
    private String source;

    /** 说明 */
    private String explanation;
}
