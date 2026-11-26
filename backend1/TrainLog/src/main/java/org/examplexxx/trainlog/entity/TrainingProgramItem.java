package org.examplexxx.trainlog.entity;

import lombok.Data;

/**
 * 训练计划项，对应数据库表 training_program_item。
 * 每项 = 星期几 + 练什么；type 为空表示该大类任意项目。
 */
@Data
public class TrainingProgramItem {

    /** 主键 */
    private Long id;

    /** 所属计划 */
    private Long programId;

    /** 训练大类：STRENGTH / CARDIO */
    private String category;

    /** 具体项目：CHEST/.../RUNNING，null 表示任意 */
    private String type;

    /** 阶段目标：BULK 增肌 / CUT 减脂 */
    private String goal;

    /** 备注 */
    private String note;

    /** 同一天内的排序 */
    private Integer sort;
}
