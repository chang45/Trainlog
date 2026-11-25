package org.examplexxx.trainlog.vo;

import lombok.Data;
import org.examplexxx.trainlog.entity.TrainingProgramItem;
import org.springframework.beans.BeanUtils;

/**
 * 计划项返回结构。
 */
@Data
public class TrainingProgramItemVO {

    private Long id;
    private String category;
    private String type;
    private String note;
    private Integer sort;

    public static TrainingProgramItemVO from(TrainingProgramItem item) {
        if (item == null) {
            return null;
        }
        TrainingProgramItemVO vo = new TrainingProgramItemVO();
        BeanUtils.copyProperties(item, vo);
        return vo;
    }
}
