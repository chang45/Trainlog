package org.examplexxx.trainlog.dto;

import lombok.Data;

import java.util.List;

/**
 * 新建 / 修改训练计划的入参。
 * items 只在新建时生效，可以一次带上默认计划项；修改计划只用 name/description。
 */
@Data
public class TrainingProgramDTO {

    /** 计划名 */
    private String name;

    /** 描述 */
    private String description;

    /** 初始计划项（选填） */
    private List<TrainingProgramItemDTO> items;
}
