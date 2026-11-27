package org.examplexxx.trainlog.controller;

import org.examplexxx.trainlog.dto.DietRecordDTO;
import org.examplexxx.trainlog.dto.DietRecognitionDTO;
import org.examplexxx.trainlog.exception.BizException;
import org.examplexxx.trainlog.result.Result;
import org.examplexxx.trainlog.service.DietRecognitionService;
import org.examplexxx.trainlog.service.DietService;
import org.examplexxx.trainlog.vo.DietRecordVO;
import org.examplexxx.trainlog.vo.DietRecognitionVO;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;

/**
 * 每日饮食：记录 + 图片识别。
 */
@RestController
@RequestMapping("/api/diet")
public class DietController {

    private final DietService dietService;
    private final DietRecognitionService recognitionService;

    public DietController(DietService dietService,
                          DietRecognitionService recognitionService) {
        this.dietService = dietService;
        this.recognitionService = recognitionService;
    }

    @GetMapping("/records")
    public Result<List<DietRecordVO>> list(@RequestParam(required = false) String date) {
        LocalDate day;
        try {
            day = StringUtils.hasText(date) ? LocalDate.parse(date) : LocalDate.now();
        } catch (DateTimeParseException e) {
            throw new BizException(400, "日期格式不正确");
        }
        return Result.success(dietService.list(day));
    }

    @PostMapping("/records")
    public Result<DietRecordVO> create(@RequestBody DietRecordDTO dto) {
        return Result.success("已保存", dietService.create(dto));
    }

    @DeleteMapping("/records/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        dietService.delete(id);
        return Result.success(null);
    }

    @PostMapping("/recognize")
    public Result<DietRecognitionVO> recognize(@RequestBody DietRecognitionDTO dto) {
        return Result.success(recognitionService.recognize(dto));
    }
}
