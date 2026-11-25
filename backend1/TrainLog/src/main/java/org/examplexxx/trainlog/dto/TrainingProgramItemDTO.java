package org.examplexxx.trainlog.dto;

import lombok.Data;

/**
 * 计划项入参：练什么。
 */
@Data
public class TrainingProgramItemDTO {

    /** 训练大类：STRENGTH / CARDIO */
    private String category;

    /** 具体项目，留空表示该大类任意 */
    private String type;

    /** 备注 */
    private String note;

    /** 同一天内的排序，选填 */
    private Integer sort;
}
