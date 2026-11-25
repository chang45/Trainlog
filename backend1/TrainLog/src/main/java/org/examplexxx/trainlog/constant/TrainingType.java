package org.examplexxx.trainlog.constant;

import lombok.Data;

import java.util.Arrays;
import java.util.Optional;

/**
 * 训练类型枚举：每个具体项目归属一个大类。
 * 编码必须和前端 utils/trainingTypes.js、数据库 training_record.type 保持一致。
 */
public enum TrainingType {

    CHEST("STRENGTH", "胸部训练"),
    SHOULDER("STRENGTH", "肩部训练"),
    BACK("STRENGTH", "背部训练"),
    LEG("STRENGTH", "腿部训练"),
    ABS("STRENGTH", "腹部训练"),
    HIPS("STRENGTH", "臀部训练"),
    INCLINE("CARDIO", "爬坡"),
    CYCLING("CARDIO", "骑车"),
    RUNNING("CARDIO", "跑步");

    private final String category;
    private final String label;

    TrainingType(String category, String label) {
        this.category = category;
        this.label = label;
    }

    public String getCategory() {
        return category;
    }

    public String getLabel() {
        return label;
    }

    public static Optional<TrainingType> find(String code) {
        if (code == null || code.isBlank()) {
            return Optional.empty();
        }
        return Arrays.stream(values())
                .filter(type -> type.name().equalsIgnoreCase(code.trim()))
                .findFirst();
    }
}
