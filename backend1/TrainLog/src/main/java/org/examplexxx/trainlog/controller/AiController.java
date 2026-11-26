package org.examplexxx.trainlog.controller;

import org.examplexxx.trainlog.dto.CaloriesRequest;
import org.examplexxx.trainlog.dto.DailyCaloriesRequest;
import org.examplexxx.trainlog.result.Result;
import org.examplexxx.trainlog.service.CaloriesService;
import org.examplexxx.trainlog.service.DailyCaloriesService;
import org.examplexxx.trainlog.vo.CaloriesVO;
import org.examplexxx.trainlog.vo.DailyCaloriesVO;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * AI 相关接口。
 */
@RestController
@RequestMapping("/api/ai")
public class AiController {

    private final CaloriesService caloriesService;
    private final DailyCaloriesService dailyCaloriesService;

    public AiController(CaloriesService caloriesService,
                        DailyCaloriesService dailyCaloriesService) {
        this.caloriesService = caloriesService;
        this.dailyCaloriesService = dailyCaloriesService;
    }

    /**
     * 估算一次训练消耗的热量（火山引擎方舟大模型）。
     *
     * <p>请求示例：
     * <pre>
     * {
     *   "type": "CHEST",
     *   "durationMinutes": 50,
     *   "sets": 4,
     *   "reps": 8,
     *   "weightKg": 60,
     *   "intensity": 7
     * }
     * </pre>
     */
    @PostMapping("/calories")
    public Result<CaloriesVO> estimateCalories(@RequestBody CaloriesRequest request) {
        return Result.success(caloriesService.estimate(request));
    }

    /**
     * 根据身高体重推算每天的基础代谢和总消耗。
     *
     * <p>请求体可以传，也可以不传：
     * <pre>
     * { "heightCm": 175, "weightKg": 68.5 }
     * </pre>
     * 不传时用当前登录用户资料里的身高体重。
     */
    @PostMapping("/daily-calories")
    public Result<DailyCaloriesVO> estimateDailyCalories(
            @RequestBody(required = false) DailyCaloriesRequest request) {
        return Result.success(dailyCaloriesService.estimate(request));
    }
}
