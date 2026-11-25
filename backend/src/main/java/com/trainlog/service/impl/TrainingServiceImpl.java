package com.trainlog.service.impl;

import com.trainlog.constant.TrainingType;
import com.trainlog.dto.TrainingRecordDTO;
import com.trainlog.entity.TrainingRecord;
import com.trainlog.exception.BizException;
import com.trainlog.mapper.TrainingRecordMapper;
import com.trainlog.result.PageResult;
import com.trainlog.service.TrainingService;
import com.trainlog.vo.TrainingRecordVO;
import com.trainlog.vo.TrainingStatsVO;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class TrainingServiceImpl implements TrainingService {

    private final TrainingRecordMapper trainingRecordMapper;

    public TrainingServiceImpl(TrainingRecordMapper trainingRecordMapper) {
        this.trainingRecordMapper = trainingRecordMapper;
    }

    @Override
    public PageResult<TrainingRecordVO> page(Long userId, LocalDate from, LocalDate to,
                                             String type, String keyword, int page, int size) {
        int safePage = Math.max(1, page);
        int safeSize = Math.min(100, Math.max(1, size));
        Sort sort = Sort.by(Sort.Direction.DESC, "trainingDate")
                .and(Sort.by(Sort.Direction.DESC, "id"));
        Page<TrainingRecord> result = trainingRecordMapper.findAll(
                filter(userId, from, to, type, keyword),
                PageRequest.of(safePage - 1, safeSize, sort));
        List<TrainingRecordVO> items = result.getContent().stream()
                .map(TrainingRecordVO::from)
                .toList();
        return new PageResult<>(items, result.getTotalElements(), safePage, safeSize,
                result.getTotalPages());
    }

    @Override
    public TrainingRecordVO get(Long userId, Long id) {
        return TrainingRecordVO.from(findOwned(userId, id));
    }

    @Override
    @Transactional
    public TrainingRecordVO create(Long userId, TrainingRecordDTO dto) {
        TrainingType type = TrainingType.require(dto.type());
        LocalDateTime now = LocalDateTime.now();
        TrainingRecord record = new TrainingRecord();
        record.setUserId(userId);
        apply(record, dto, type);
        record.setCreatedAt(now);
        record.setUpdatedAt(now);
        return TrainingRecordVO.from(trainingRecordMapper.save(record));
    }

    @Override
    @Transactional
    public TrainingRecordVO update(Long userId, Long id, TrainingRecordDTO dto) {
        TrainingRecord record = findOwned(userId, id);
        apply(record, dto, TrainingType.require(dto.type()));
        record.setUpdatedAt(LocalDateTime.now());
        return TrainingRecordVO.from(trainingRecordMapper.save(record));
    }

    @Override
    @Transactional
    public void delete(Long userId, Long id) {
        trainingRecordMapper.delete(findOwned(userId, id));
    }

    @Override
    public TrainingStatsVO stats(Long userId, int days) {
        int safeDays = Math.min(365, Math.max(1, days));
        LocalDate today = LocalDate.now();
        LocalDate start = today.minusDays(safeDays - 1L);

        List<TrainingRecord> range = trainingRecordMapper.findAll(
                filter(userId, start, today, null, null));

        Map<LocalDate, List<TrainingRecord>> byDate = range.stream()
                .collect(Collectors.groupingBy(TrainingRecord::getTrainingDate));

        List<TrainingStatsVO.DailyPoint> daily = new ArrayList<>(safeDays);
        for (LocalDate date = start; !date.isAfter(today); date = date.plusDays(1)) {
            List<TrainingRecord> dayRecords = byDate.getOrDefault(date, List.of());
            int minutes = dayRecords.stream().mapToInt(TrainingRecord::getDurationMinutes).sum();
            double distance = dayRecords.stream()
                    .map(TrainingRecord::getDistanceKm)
                    .filter(d -> d != null)
                    .mapToDouble(Double::doubleValue)
                    .sum();
            daily.add(new TrainingStatsVO.DailyPoint(date, minutes, dayRecords.size(), round(distance)));
        }

        Map<TrainingType, int[]> typeNumbers = new LinkedHashMap<>();
        Map<TrainingType, Double> typeDistance = new LinkedHashMap<>();
        for (TrainingRecord record : range) {
            TrainingType type = TrainingType.find(record.getType()).orElse(TrainingType.OTHER);
            int[] numbers = typeNumbers.computeIfAbsent(type, t -> new int[2]);
            numbers[0]++;
            numbers[1] += record.getDurationMinutes();
            double distance = record.getDistanceKm() == null ? 0 : record.getDistanceKm();
            typeDistance.merge(type, distance, Double::sum);
        }
        List<TrainingStatsVO.TypeStat> byType = typeNumbers.entrySet().stream()
                .map(e -> new TrainingStatsVO.TypeStat(e.getKey().name(), e.getKey().getLabel(),
                        e.getValue()[0], e.getValue()[1], round(typeDistance.get(e.getKey()))))
                .toList();

        int totalMinutes = range.stream().mapToInt(TrainingRecord::getDurationMinutes).sum();
        double totalDistance = range.stream()
                .map(TrainingRecord::getDistanceKm)
                .filter(d -> d != null)
                .mapToDouble(Double::doubleValue)
                .sum();
        int totalCalories = range.stream()
                .map(TrainingRecord::getCalories)
                .filter(c -> c != null)
                .mapToInt(Integer::intValue)
                .sum();

        return new TrainingStatsVO(safeDays, range.size(), totalMinutes, round(totalDistance),
                totalCalories, streak(userId, today), daily, byType);
    }

    private int streak(Long userId, LocalDate today) {
        Set<LocalDate> dates = Set.copyOf(trainingRecordMapper.findTrainingDates(userId));
        if (dates.isEmpty()) {
            return 0;
        }
        LocalDate cursor = dates.contains(today) ? today : today.minusDays(1);
        int streak = 0;
        while (dates.contains(cursor)) {
            streak++;
            cursor = cursor.minusDays(1);
        }
        return streak;
    }

    private Specification<TrainingRecord> filter(Long userId, LocalDate from, LocalDate to,
                                                 String type, String keyword) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("userId"), userId));
            if (from != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("trainingDate"), from));
            }
            if (to != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("trainingDate"), to));
            }
            if (type != null && !type.isBlank()) {
                predicates.add(cb.equal(root.get("type"), TrainingType.require(type).name()));
            }
            if (keyword != null && !keyword.isBlank()) {
                String like = "%" + keyword.trim().toLowerCase(Locale.ROOT) + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("title")), like),
                        cb.like(cb.lower(cb.coalesce(root.get("notes"), "")), like)));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    private void apply(TrainingRecord record, TrainingRecordDTO dto, TrainingType type) {
        if (dto.trainingDate().isAfter(LocalDate.now())) {
            throw new BizException(400, "训练日期不能晚于今天");
        }
        record.setTitle(dto.title().trim());
        record.setType(type.name());
        record.setTrainingDate(dto.trainingDate());
        record.setDurationMinutes(dto.durationMinutes());
        record.setDistanceKm(dto.distanceKm());
        record.setCalories(dto.calories());
        record.setIntensity(dto.intensity());
        record.setNotes(dto.notes() == null || dto.notes().isBlank() ? null : dto.notes().trim());
    }

    private TrainingRecord findOwned(Long userId, Long id) {
        TrainingRecord record = trainingRecordMapper.findById(id)
                .orElseThrow(() -> new BizException(404, "训练记录不存在"));
        if (!record.getUserId().equals(userId)) {
            throw new BizException(404, "训练记录不存在");
        }
        return record;
    }

    private double round(double value) {
        return Math.round(value * 10.0) / 10.0;
    }
}
