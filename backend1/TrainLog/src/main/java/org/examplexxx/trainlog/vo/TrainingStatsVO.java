package org.examplexxx.trainlog.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * 训练统计返回结构，对应前端概览页 / 统计页。
 */
@Data
public class TrainingStatsVO {

    /** 时间段内训练次数 */
    private int sessions;

    /** 总时长（分钟） */
    private int totalMinutes;

    /** 总距离（公里） */
    private BigDecimal totalDistanceKm = BigDecimal.ZERO;

    /** 总消耗（千卡） */
    private int totalCalories;

    /** 连续打卡天数 */
    private int streakDays;

    /** 每日训练量 */
    private List<DailyStat> daily = new ArrayList<>();

    /** 按具体项目的分布 */
    private List<TypeStat> byType = new ArrayList<>();

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DailyStat {
        private LocalDate date;
        private int sessions;
        private int minutes;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TypeStat {
        private String type;
        private int minutes;
        private int sessions;
    }
}
