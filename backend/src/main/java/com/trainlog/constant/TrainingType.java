package com.trainlog.constant;

import com.trainlog.exception.BizException;

import java.util.Arrays;
import java.util.Optional;

public enum TrainingType {

    STRENGTH("力量"),
    RUNNING("跑步"),
    CYCLING("骑行"),
    SWIMMING("游泳"),
    STRETCHING("拉伸"),
    HIIT("HIIT"),
    BALL("球类"),
    OTHER("其他");

    private final String label;

    TrainingType(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    public static Optional<TrainingType> find(String value) {
        if (value == null || value.isBlank()) {
            return Optional.empty();
        }
        return Arrays.stream(values())
                .filter(t -> t.name().equalsIgnoreCase(value.trim()))
                .findFirst();
    }

    public static TrainingType require(String value) {
        return find(value).orElseThrow(() -> new BizException(400, "未知的训练类型"));
    }

    public static String labelOf(String value) {
        return find(value).map(TrainingType::getLabel).orElse(value);
    }
}
