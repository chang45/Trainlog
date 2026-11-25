package org.examplexxx.trainlog.controller;

import org.examplexxx.trainlog.dto.TrainingProgramDTO;
import org.examplexxx.trainlog.dto.TrainingProgramItemDTO;
import org.examplexxx.trainlog.result.Result;
import org.examplexxx.trainlog.service.TrainingProgramService;
import org.examplexxx.trainlog.vo.TrainingProgramItemVO;
import org.examplexxx.trainlog.vo.TrainingProgramVO;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 训练计划接口。每个用户只能操作自己的计划（userId 从 token 取）。
 */
@RestController
@RequestMapping("/api/training/programs")
public class TrainingProgramController {

    private final TrainingProgramService trainingProgramService;

    public TrainingProgramController(TrainingProgramService trainingProgramService) {
        this.trainingProgramService = trainingProgramService;
    }

    /** 我的计划列表（含计划项） */
    @GetMapping
    public Result<List<TrainingProgramVO>> list() {
        return Result.success(trainingProgramService.list());
    }

    /** 新建计划，可带初始计划项 */
    @PostMapping
    public Result<TrainingProgramVO> create(@RequestBody TrainingProgramDTO dto) {
        return Result.success("计划已创建", trainingProgramService.create(dto));
    }

    /** 改计划名 / 描述 */
    @PutMapping("/{id}")
    public Result<TrainingProgramVO> update(@PathVariable Long id,
                                            @RequestBody TrainingProgramDTO dto) {
        return Result.success("计划已更新", trainingProgramService.update(id, dto));
    }

    /** 删除计划（计划项跟着级联删除） */
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        trainingProgramService.delete(id);
        return Result.success("计划已删除", null);
    }

    /** 设为当前计划 */
    @PutMapping("/{id}/active")
    public Result<TrainingProgramVO> active(@PathVariable Long id) {
        return Result.success("已设为当前计划", trainingProgramService.setActive(id));
    }

    /** 加计划项 */
    @PostMapping("/{id}/items")
    public Result<TrainingProgramItemVO> addItem(@PathVariable Long id,
                                                 @RequestBody TrainingProgramItemDTO dto) {
        return Result.success("计划项已添加", trainingProgramService.addItem(id, dto));
    }

    /** 改计划项 */
    @PutMapping("/{id}/items/{itemId}")
    public Result<TrainingProgramItemVO> updateItem(@PathVariable Long id,
                                                    @PathVariable Long itemId,
                                                    @RequestBody TrainingProgramItemDTO dto) {
        return Result.success("计划项已更新", trainingProgramService.updateItem(id, itemId, dto));
    }

    /** 删计划项 */
    @DeleteMapping("/{id}/items/{itemId}")
    public Result<Void> deleteItem(@PathVariable Long id, @PathVariable Long itemId) {
        trainingProgramService.deleteItem(id, itemId);
        return Result.success("计划项已删除", null);
    }
}
