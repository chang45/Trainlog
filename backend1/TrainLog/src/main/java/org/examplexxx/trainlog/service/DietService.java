package org.examplexxx.trainlog.service;

import org.examplexxx.trainlog.dto.DietRecordDTO;
import org.examplexxx.trainlog.vo.DietRecordVO;

import java.time.LocalDate;
import java.util.List;

public interface DietService {

    List<DietRecordVO> list(LocalDate date);

    DietRecordVO create(DietRecordDTO dto);

    void delete(Long id);
}
