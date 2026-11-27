package org.examplexxx.trainlog.vo;

import lombok.Data;
import org.examplexxx.trainlog.entity.TrainingProgram;
import org.springframework.beans.BeanUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * 训练计划返回结构，带计划项。
 */
@Data
public class TrainingProgramVO {

    private Long id;
    private String name;
    private String description;
    private String goal;

    /** 是否当前计划 */
    private Boolean active;

    private List<TrainingProgramItemVO> items = new ArrayList<>();

    public static TrainingProgramVO from(TrainingProgram program) {
        if (program == null) {
            return null;
        }
        TrainingProgramVO vo = new TrainingProgramVO();
        BeanUtils.copyProperties(program, vo);
        return vo;
    }
}
