package org.examplexxx.trainlog.vo;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * 热量估算结果。
 */
@Data
@AllArgsConstructor
public class CaloriesVO {

    /** 估算消耗（千卡） */
    private int calories;

    /** 来源：AI-大模型估算 / FORMULA-MET 公式兜底 */
    private String source;

    /** 说明 */
    private String explanation;
}
