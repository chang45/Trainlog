package org.examplexxx.trainlog.service;

import org.examplexxx.trainlog.dto.TrainingProgramDTO;
import org.examplexxx.trainlog.dto.TrainingProgramItemDTO;
import org.examplexxx.trainlog.vo.TrainingProgramItemVO;
import org.examplexxx.trainlog.vo.TrainingProgramVO;

import java.util.List;

/**
 * 训练计划业务：每个用户只能操作自己的计划。
 */
public interface TrainingProgramService {

    /** 我的计划列表（含计划项） */
    List<TrainingProgramVO> list();

    TrainingProgramVO create(TrainingProgramDTO dto);

    TrainingProgramVO update(Long id, TrainingProgramDTO dto);

    void delete(Long id);

    /** 设为当前计划，自动取消其它计划的 active */
    TrainingProgramVO setActive(Long id);

    TrainingProgramItemVO addItem(Long programId, TrainingProgramItemDTO dto);

    TrainingProgramItemVO updateItem(Long programId, Long itemId, TrainingProgramItemDTO dto);

    void deleteItem(Long programId, Long itemId);
}
