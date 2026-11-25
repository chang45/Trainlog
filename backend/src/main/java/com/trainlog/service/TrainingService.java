package com.trainlog.service;

import com.trainlog.dto.TrainingRecordDTO;
import com.trainlog.result.PageResult;
import com.trainlog.vo.TrainingRecordVO;
import com.trainlog.vo.TrainingStatsVO;

import java.time.LocalDate;

public interface TrainingService {

    PageResult<TrainingRecordVO> page(Long userId, LocalDate from, LocalDate to,
                                      String type, String keyword, int page, int size);

    TrainingRecordVO get(Long userId, Long id);

    TrainingRecordVO create(Long userId, TrainingRecordDTO dto);

    TrainingRecordVO update(Long userId, Long id, TrainingRecordDTO dto);

    void delete(Long userId, Long id);

    TrainingStatsVO stats(Long userId, int days);
}
