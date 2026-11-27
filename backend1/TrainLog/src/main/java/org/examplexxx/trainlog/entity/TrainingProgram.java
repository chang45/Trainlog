package org.examplexxx.trainlog.entity;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 训练计划实体，对应数据库表 training_program。
 * 一个用户可以有多个计划，active=true 表示当前计划。
 */
@Data
public class TrainingProgram {

    /** 主键 */
    private Long id;

    /** 所属用户 */
    private Long userId;

    /** 计划名，例如：一周四练-增肌 */
    private String name;

    /** 描述 */
    private String description;

    /** 计划目标：BULK 增肌 / CUT 减脂 / MAINTAIN 维持 */
    private String goal;

    /** 是否当前计划：每人最多一个 true */
    private Boolean active;

    /** 创建时间 */
    private LocalDateTime createdAt;

    /** 更新时间 */
    private LocalDateTime updatedAt;
}
