package com.trainlog.vo;

import com.trainlog.constant.TrainingType;
import com.trainlog.entity.TrainingRecord;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record TrainingRecordVO(
        Long id,
        String title,
        String type,
        String typeLabel,
        LocalDate trainingDate,
        int durationMinutes,
        Double distanceKm,
        Integer calories,
        Integer intensity,
        String notes,
        LocalDateTime createdAt
) {
    public static TrainingRecordVO from(TrainingRecord record) {
        return new TrainingRecordVO(
                record.getId(),
                record.getTitle(),
                record.getType(),
                TrainingType.labelOf(record.getType()),
                record.getTrainingDate(),
                record.getDurationMinutes(),
                record.getDistanceKm(),
                record.getCalories(),
                record.getIntensity(),
                record.getNotes(),
                record.getCreatedAt());
    }
}
