package org.examplexxx.trainlog.dto;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 完善个人资料入参：身高、体重、年龄、性别。
 */
@Data
public class ProfileDTO {

    /** 身高（厘米） */
    private BigDecimal heightCm;

    /** 体重（公斤） */
    private BigDecimal weightKg;

    /** 年龄 */
    private Integer age;

    /** 性别：MALE / FEMALE */
    private String gender;
}
