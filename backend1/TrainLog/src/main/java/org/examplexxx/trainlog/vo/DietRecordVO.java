package org.examplexxx.trainlog.vo;

import lombok.Data;
import org.examplexxx.trainlog.entity.DietRecord;
import org.springframework.beans.BeanUtils;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 饮食记录返回结构。
 */
@Data
public class DietRecordVO {

    private Long id;
    private String mealType;
    private String foodName;
    private Integer calories;
    private BigDecimal protein;
    private BigDecimal carbs;
    private BigDecimal fat;
    private LocalDate eatenDate;

    public static DietRecordVO from(DietRecord record) {
        DietRecordVO vo = new DietRecordVO();
        BeanUtils.copyProperties(record, vo);
        return vo;
    }
}
