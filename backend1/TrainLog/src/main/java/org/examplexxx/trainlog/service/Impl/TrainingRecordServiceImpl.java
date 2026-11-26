package org.examplexxx.trainlog.service.Impl;

import org.examplexxx.trainlog.constant.TrainingType;
import org.examplexxx.trainlog.context.BaseContext;
import org.examplexxx.trainlog.dto.TrainingRecordDTO;
import org.examplexxx.trainlog.dto.TrainingRecordQueryDTO;
import org.examplexxx.trainlog.entity.TrainingRecord;
import org.examplexxx.trainlog.exception.BizException;
import org.examplexxx.trainlog.mapper.TrainingRecordMapper;
import org.examplexxx.trainlog.result.PageResult;
import org.examplexxx.trainlog.service.TrainingRecordService;
import org.examplexxx.trainlog.vo.TrainingRecordVO;
import org.examplexxx.trainlog.vo.TrainingStatsVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class TrainingRecordServiceImpl implements TrainingRecordService {

    @Autowired
    private TrainingRecordMapper trainingRecordMapper;

    @Override
    public TrainingRecordVO create(TrainingRecordDTO dto) {
        Long userId =currentUserId();
        TrainingRecord record = new TrainingRecord();
        apply(dto, record);
        record.setUserId(userId);
        record.setCreatedAt(LocalDateTime.now());
        record.setUpdatedAt(LocalDateTime.now());
        trainingRecordMapper.insert(record);
        return TrainingRecordVO.from(record);
    }

    @Override
    public PageResult<TrainingRecordVO> page(TrainingRecordQueryDTO query) {
        Long userId = currentUserId();
        TrainingRecordQueryDTO q = query == null ? new TrainingRecordQueryDTO() : query;
        q.setPage(q.normalizedPage());
        q.setSize(q.normalizedSize());

        long total = trainingRecordMapper.countPage(userId, q);
        List<TrainingRecordVO> items = new ArrayList<>();
        if (total > 0) {
            for (TrainingRecord record : trainingRecordMapper.findPage(userId, q)) {
                items.add(TrainingRecordVO.from(record));
            }
        }
        return PageResult.of(items, total, q.getSize());
    }

    @Override
    public TrainingRecordVO update(Long id, TrainingRecordDTO dto) {
        Long userId = currentUserId();
        if (id == null) {
            throw new BizException(400, "缺少记录 id");
        }
        TrainingRecord record = trainingRecordMapper.findById(id, userId);
        if (record == null) {
            throw new BizException(404, "记录不存在或不属于你");
        }
        apply(dto, record);
        record.setUpdatedAt(LocalDateTime.now());
        trainingRecordMapper.update(record);
        return TrainingRecordVO.from(record);
    }

    @Override
    public void delete(Long id) {
        Long userId = currentUserId();
        if (id == null || trainingRecordMapper.delete(id, userId) == 0) {
            throw new BizException(404, "记录不存在或不属于你");
        }
    }

    @Override
    public TrainingStatsVO stats(int days) {
        Long userId = currentUserId();
        int span = Math.min(Math.max(days, 1), 365);
        LocalDate to = LocalDate.now();
        LocalDate from = to.minusDays(span - 1L);

        TrainingStatsVO stats = trainingRecordMapper.totals(userId, from, to);
        if (stats == null) {
            stats = new TrainingStatsVO();
        }

        // SQL 只会返回有记录的日期，这里补零，保证前端柱状图每天都有柱子
        Map<LocalDate, TrainingStatsVO.DailyStat> byDate = new HashMap<>();
        for (TrainingStatsVO.DailyStat item : trainingRecordMapper.daily(userId, from, to)) {
            byDate.put(item.getDate(), item);
        }
        List<TrainingStatsVO.DailyStat> filled = new ArrayList<>();
        for (LocalDate date = from; !date.isAfter(to); date = date.plusDays(1)) {
            TrainingStatsVO.DailyStat item = byDate.get(date);
            filled.add(item != null ? item : new TrainingStatsVO.DailyStat(date, 0, 0, 0));
        }
        stats.setDaily(filled);
        stats.setByType(trainingRecordMapper.byType(userId, from, to));
        stats.setStreakDays(streak(userId));
        if (stats.getTotalDistanceKm() == null) {
            stats.setTotalDistanceKm(BigDecimal.ZERO);
        }
        return stats;
    }

    /**
     * 连续打卡：今天有记录就从今天往前数，今天没有就从昨天开始数（不打断连续）。
     */
    private int streak(Long userId) {
        List<LocalDate> dates = trainingRecordMapper.recentDates(userId);
        if (dates == null || dates.isEmpty()) {
            return 0;
        }
        Set<LocalDate> recorded = new HashSet<>(dates);
        LocalDate cursor = LocalDate.now();
        if (!recorded.contains(cursor)) {
            cursor = cursor.minusDays(1);
        }
        int count = 0;
        while (recorded.contains(cursor)) {
            count++;
            cursor = cursor.minusDays(1);
        }
        return count;
    }

    /**
     * 校验入参并写入实体。力量训练和 有氧训练 的量化字段互斥：
     * 力量 -> sets/reps/weight，有氧 -> distance。
     */
    private void apply(TrainingRecordDTO dto, TrainingRecord record) {
        if (dto == null) {
            throw new BizException(400, "请求体不能为空");
        }
        String title = dto.getTitle() == null ? "" : dto.getTitle().trim();
        if (title.isEmpty()) {
            throw new BizException(400, "请填写训练项目");
        }
        if (title.length() > 80) {
            throw new BizException(400, "训练项目最多 80 个字符");
        }
        if (dto.getTrainingDate() == null) {
            throw new BizException(400, "请选择训练日期");
        }
        if (dto.getDurationMinutes() == null || dto.getDurationMinutes() < 1) {
            throw new BizException(400, "训练时长至少 1 分钟");
        }
        if (dto.getIntensity() != null && (dto.getIntensity() < 1 || dto.getIntensity() > 10)) {
            throw new BizException(400, "主观强度需在 1-10 之间");
        }
        if (dto.getCalories() != null && dto.getCalories() < 0) {
            throw new BizException(400, "消耗热量不能为负数");
        }
        if (dto.getNotes() != null && dto.getNotes().length() > 500) {
            throw new BizException(400, "备注最多 500 个字符");
        }

        TrainingType type = TrainingType.find(dto.getType())
                .orElseThrow(() -> new BizException(400, "未知的训练类型"));
        if (dto.getCategory() != null && !dto.getCategory().isBlank()
                && !type.getCategory().equalsIgnoreCase(dto.getCategory().trim())) {
            throw new BizException(400, "训练大类和具体项目不匹配");
        }

        record.setTitle(title);
        record.setCategory(type.getCategory());
        record.setType(type.name());
        record.setTrainingDate(dto.getTrainingDate());
        record.setDurationMinutes(dto.getDurationMinutes());
        record.setCalories(dto.getCalories());
        record.setIntensity(dto.getIntensity());
        record.setNotes(dto.getNotes() == null ? null : dto.getNotes().trim());

        if ("STRENGTH".equals(type.getCategory())) {
            if (dto.getSets() == null || dto.getSets() < 1) {
                throw new BizException(400, "力量训练请填写组数");
            }
            if (dto.getReps() == null || dto.getReps() < 1) {
                throw new BizException(400, "力量训练请填写每组次数");
            }
            if (dto.getWeightKg() != null && dto.getWeightKg().compareTo(BigDecimal.ZERO) < 0) {
                throw new BizException(400, "重量不能为负数");
            }
            record.setSets(dto.getSets());
            record.setReps(dto.getReps());
            record.setWeightKg(dto.getWeightKg());
            record.setDistanceKm(null);
        } else {
            if (dto.getDistanceKm() != null && dto.getDistanceKm().compareTo(BigDecimal.ZERO) < 0) {
                throw new BizException(400, "距离不能为负数");
            }
            record.setDistanceKm(dto.getDistanceKm());
            record.setSets(null);
            record.setReps(null);
            record.setWeightKg(null);
        }
    }

    private Long currentUserId() {
        Long userId = BaseContext.getCurrentId();
        if (userId == null) {
            throw new BizException(401, "请先登录");
        }
        return userId;
    }
}
