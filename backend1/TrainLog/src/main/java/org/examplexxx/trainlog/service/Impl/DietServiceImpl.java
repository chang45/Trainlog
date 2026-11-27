package org.examplexxx.trainlog.service.Impl;

import lombok.Data;
import org.examplexxx.trainlog.context.BaseContext;
import org.examplexxx.trainlog.dto.DietRecordDTO;
import org.examplexxx.trainlog.entity.DietRecord;
import org.examplexxx.trainlog.exception.BizException;
import org.examplexxx.trainlog.mapper.DietRecordMapper;
import org.examplexxx.trainlog.service.DietService;
import org.examplexxx.trainlog.vo.DietRecordVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Data
@Service
public class DietServiceImpl implements DietService {

    private static final Set<String> MEAL_TYPES = Set.of("BREAKFAST", "LUNCH", "DINNER");

    @Autowired
    private DietRecordMapper dietRecordMapper;


    @Override
    public List<DietRecordVO> list(LocalDate date) {
        Long userId = currentUserId();
        LocalDate day = date == null ? LocalDate.now() : date;
        return dietRecordMapper.findByUserAndDate(userId, day)
                .stream()
                .map(DietRecordVO::from)
                .toList();
    }

    @Override
    public DietRecordVO create(DietRecordDTO dto) {
        Long userId = currentUserId();
        if (dto == null) {
            throw new BizException(400, "请求体不能为空");
        }
        String mealType = normalizeMealType(dto.getMealType());
        if (!StringUtils.hasText(dto.getFoodName())) {
            throw new BizException(400, "请填写食物名");
        }
        String foodName = dto.getFoodName().trim();
        if (foodName.length() > 100) {
            throw new BizException(400, "食物名最多 100 个字符");
        }
        if (dto.getCalories() != null && (dto.getCalories() < 0 || dto.getCalories() > 5000)) {
            throw new BizException(400, "热量需在 0-5000 千卡之间");
        }
        BigDecimal protein = checkMacro(dto.getProtein(), "蛋白质");
        BigDecimal carbs = checkMacro(dto.getCarbs(), "碳水");
        BigDecimal fat = checkMacro(dto.getFat(), "脂肪");

        DietRecord record = new DietRecord();
        record.setUserId(userId);
        record.setMealType(mealType);
        record.setFoodName(foodName);
        record.setCalories(dto.getCalories());
        record.setProtein(protein);
        record.setCarbs(carbs);
        record.setFat(fat);
        record.setEatenDate(dto.getEatenDate() == null ? LocalDate.now() : dto.getEatenDate());
        record.setCreatedAt(LocalDateTime.now());
        dietRecordMapper.insert(record);
        return DietRecordVO.from(record);
    }

    @Override
    public void delete(Long id) {
        Long userId = currentUserId();
        if (id == null || dietRecordMapper.delete(id, userId) == 0) {
            throw new BizException(404, "饮食记录不存在或不属于你");
        }
    }

    private String normalizeMealType(String mealType) {
        if (!StringUtils.hasText(mealType)) {
            throw new BizException(400, "请选择餐次");
        }
        String code = mealType.trim().toUpperCase(Locale.ROOT);
        if (!MEAL_TYPES.contains(code)) {
            throw new BizException(400, "餐次只能是早餐、午餐或晚餐");
        }
        return code;
    }

    private BigDecimal checkMacro(BigDecimal value, String label) {
        if (value == null) {
            return null;
        }
        if (value.compareTo(BigDecimal.ZERO) < 0 || value.compareTo(BigDecimal.valueOf(500)) > 0) {
            throw new BizException(400, label + "需在 0-500 克之间");
        }
        return value;
    }

    private Long currentUserId() {
        Long userId = BaseContext.getCurrentId();
        if (userId == null) {
            throw new BizException(401, "请先登录");
        }
        return userId;
    }
}
