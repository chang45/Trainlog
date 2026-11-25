package org.examplexxx.trainlog.vo;

import lombok.Data;
import org.examplexxx.trainlog.entity.TrainingRecord;
import org.springframework.beans.BeanUtils;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 训练记录返回给前端的结构。
 * 注意不要直接把 entity 返回出去（里面有 userId 等内部字段）。
 */
@Data
public class TrainingRecordVO {

    private Long id;
    private String title;
    private String category;
    private String type;
    private LocalDate trainingDate;
    private Integer durationMinutes;
    private BigDecimal distanceKm;
    private Integer sets;
    private Integer reps;
    private BigDecimal weightKg;
    private Integer calories;
    private Integer intensity;
    private String notes;

    public static TrainingRecordVO from(TrainingRecord record) {
        if (record == null) {
            return null;
        }
        TrainingRecordVO vo = new TrainingRecordVO();
        BeanUtils.copyProperties(record, vo);
        return vo;
    }
}
