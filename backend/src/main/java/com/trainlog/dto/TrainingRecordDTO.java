package com.trainlog.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record TrainingRecordDTO(
        @NotBlank(message = "请填写训练项目")
        @Size(max = 80, message = "训练项目最多 80 个字符")
        String title,

        @NotBlank(message = "请选择训练类型")
        String type,

        @NotNull(message = "请选择训练日期")
        LocalDate trainingDate,

        @NotNull(message = "请填写训练时长")
        @Min(value = 1, message = "训练时长至少 1 分钟")
        @Max(value = 1440, message = "训练时长不能超过 1440 分钟")
        Integer durationMinutes,

        @DecimalMin(value = "0", message = "距离不能为负数")
        @DecimalMax(value = "1000", message = "距离不能超过 1000 公里")
        Double distanceKm,

        @Min(value = 0, message = "消耗热量不能为负数")
        @Max(value = 20000, message = "消耗热量超出范围")
        Integer calories,

        @Min(value = 1, message = "强度范围 1-10")
        @Max(value = 10, message = "强度范围 1-10")
        Integer intensity,

        @Size(max = 500, message = "备注最多 500 个字符")
        String notes
) {
}
