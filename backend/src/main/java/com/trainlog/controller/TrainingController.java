package com.trainlog.controller;

import com.trainlog.constant.TrainingType;
import com.trainlog.context.BaseContext;
import com.trainlog.dto.TrainingRecordDTO;
import com.trainlog.exception.BizException;
import com.trainlog.result.PageResult;
import com.trainlog.result.Result;
import com.trainlog.service.TrainingService;
import com.trainlog.vo.TrainingRecordVO;
import com.trainlog.vo.TrainingStatsVO;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/training")
public class TrainingController {

    private final TrainingService trainingService;

    public TrainingController(TrainingService trainingService) {
        this.trainingService = trainingService;
    }

    @GetMapping("/types")
    public Result<List<Map<String, String>>> types() {
        return Result.success(Arrays.stream(TrainingType.values())
                .map(t -> Map.of("value", t.name(), "label", t.getLabel()))
                .toList());
    }

    @GetMapping("/records")
    public Result<PageResult<TrainingRecordVO>> records(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return Result.success(trainingService.page(currentUserId(), from, to, type, keyword, page, size));
    }

    @GetMapping("/records/{id}")
    public Result<TrainingRecordVO> record(@PathVariable Long id) {
        return Result.success(trainingService.get(currentUserId(), id));
    }

    @PostMapping("/records")
    public Result<TrainingRecordVO> create(@Valid @RequestBody TrainingRecordDTO dto) {
        return Result.success("记录已保存", trainingService.create(currentUserId(), dto));
    }

    @PutMapping("/records/{id}")
    public Result<TrainingRecordVO> update(@PathVariable Long id,
                                           @Valid @RequestBody TrainingRecordDTO dto) {
        return Result.success("记录已更新", trainingService.update(currentUserId(), id, dto));
    }

    @DeleteMapping("/records/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        trainingService.delete(currentUserId(), id);
        return Result.success("记录已删除", null);
    }

    @GetMapping("/stats")
    public Result<TrainingStatsVO> stats(@RequestParam(defaultValue = "30") int days) {
        return Result.success(trainingService.stats(currentUserId(), days));
    }

    private Long currentUserId() {
        Long userId = BaseContext.getCurrentId();
        if (userId == null) {
            throw new BizException(401, "请先登录");
        }
        return userId;
    }
}
