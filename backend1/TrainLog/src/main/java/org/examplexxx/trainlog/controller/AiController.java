package org.examplexxx.trainlog.controller;

import org.examplexxx.trainlog.dto.CaloriesDTO;
import org.examplexxx.trainlog.dto.DailyCaloriesDTO;
import org.examplexxx.trainlog.dto.ProteinDTO;
import org.examplexxx.trainlog.result.Result;
import org.examplexxx.trainlog.service.CaloriesService;
import org.examplexxx.trainlog.service.DailyCaloriesService;
import org.examplexxx.trainlog.service.ProteinService;
import org.examplexxx.trainlog.vo.CaloriesVO;
import org.examplexxx.trainlog.vo.DailyCaloriesVO;
import org.examplexxx.trainlog.vo.ProteinVO;
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
    private final ProteinService proteinService;

    public AiController(CaloriesService caloriesService,
                        DailyCaloriesService dailyCaloriesService,
                        ProteinService proteinService) {
        this.caloriesService = caloriesService;
        this.dailyCaloriesService = dailyCaloriesService;
        this.proteinService = proteinService;
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
    public Result<CaloriesVO> estimateCalories(@RequestBody CaloriesDTO request) {
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
            @RequestBody(required = false) DailyCaloriesDTO request) {
        return Result.success(dailyCaloriesService.estimate(request));
    }

    /**
     * 推算每天正常维持和增肌所需的蛋白质。
     *
     * <p>请求体可不传，默认用当前登录用户资料：
     * <pre>
     * { "heightCm": 175, "weightKg": 68.5, "age": 28, "gender": "MALE" }
     * </pre>
     */
    @PostMapping("/protein")
    public Result<ProteinVO> estimateProtein(
            @RequestBody(required = false) ProteinDTO request) {
        return Result.success(proteinService.estimate(request));
    }
}
