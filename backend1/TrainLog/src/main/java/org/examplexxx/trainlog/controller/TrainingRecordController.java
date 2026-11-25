package org.examplexxx.trainlog.controller;

import org.examplexxx.trainlog.dto.TrainingRecordDTO;
import org.examplexxx.trainlog.dto.TrainingRecordQueryDTO;
import org.examplexxx.trainlog.result.PageResult;
import org.examplexxx.trainlog.result.Result;
import org.examplexxx.trainlog.service.TrainingRecordService;
import org.examplexxx.trainlog.vo.TrainingRecordVO;
import org.examplexxx.trainlog.vo.TrainingStatsVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 训练记录接口。当前登录用户由 JwtTokenAdminInterceptor 解析 admin-token 后写入 BaseContext。
 */
@RestController
@RequestMapping("/api/training")
public class TrainingRecordController {

    @Autowired
    private TrainingRecordService trainingRecordService;

    /** 新增训练记录 */
    @PostMapping("/records")
    public Result<TrainingRecordVO> create(@RequestBody TrainingRecordDTO dto) {
        return Result.success("记录已保存", trainingRecordService.create(dto));
    }

    /** 分页查询（page/size/keyword/type/from/to） */
    @GetMapping("/records")
    public Result<PageResult<TrainingRecordVO>> page(TrainingRecordQueryDTO query) {
        return Result.success(trainingRecordService.page(query));
    }

    /** 修改训练记录 */
    @PutMapping("/records/{id}")
    public Result<TrainingRecordVO> update(@PathVariable Long id,
                                           @RequestBody TrainingRecordDTO dto) {
        return Result.success("记录已更新", trainingRecordService.update(id, dto));
    }

    /** 删除训练记录 */
    @DeleteMapping("/records/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        trainingRecordService.delete(id);
        return Result.success("记录已删除", null);
    }

    /** 统计：概览 / 统计页用，days 默认 7 天 */
    @GetMapping("/stats")
    public Result<TrainingStatsVO> stats(@RequestParam(defaultValue = "7") int days) {
        return Result.success(trainingRecordService.stats(days));
    }
}
