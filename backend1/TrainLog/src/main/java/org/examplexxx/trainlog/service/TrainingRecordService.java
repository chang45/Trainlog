package org.examplexxx.trainlog.service;

import org.examplexxx.trainlog.dto.TrainingRecordDTO;
import org.examplexxx.trainlog.dto.TrainingRecordQueryDTO;
import org.examplexxx.trainlog.result.PageResult;
import org.examplexxx.trainlog.vo.TrainingRecordVO;
import org.examplexxx.trainlog.vo.TrainingStatsVO;

/**
 * 训练记录业务：增删改查 + 统计。
 * 所有方法都只操作当前登录用户自己的数据。
 */
public interface TrainingRecordService {

    TrainingRecordVO create(TrainingRecordDTO dto);

    PageResult<TrainingRecordVO> page(TrainingRecordQueryDTO query);

    TrainingRecordVO update(Long id, TrainingRecordDTO dto);

    void delete(Long id);

    TrainingStatsVO stats(int days);
}
