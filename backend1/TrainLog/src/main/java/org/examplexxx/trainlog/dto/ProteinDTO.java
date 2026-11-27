package org.examplexxx.trainlog.dto;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 每日蛋白质推算入参。
 * 不传的字段用当前登录用户资料里的值。
 */
@Data
public class ProteinDTO {

    private BigDecimal heightCm;

    private BigDecimal weightKg;

    private Integer age;

    /** MALE / FEMALE */
    private String gender;
}
