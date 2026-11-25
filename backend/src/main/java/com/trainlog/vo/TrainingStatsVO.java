package com.trainlog.vo;

import java.time.LocalDate;
import java.util.List;

public record TrainingStatsVO(
        int days,
        int sessions,
        int totalMinutes,
        double totalDistanceKm,
        int totalCalories,
        int streakDays,
        List<DailyPoint> daily,
        List<TypeStat> byType
) {
    public record DailyPoint(LocalDate date, int minutes, int sessions, double distanceKm) {
    }

    public record TypeStat(String type, String label, int sessions, int minutes, double distanceKm) {
    }
}
