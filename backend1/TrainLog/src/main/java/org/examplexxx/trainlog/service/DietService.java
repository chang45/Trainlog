package org.examplexxx.trainlog.service;

import org.examplexxx.trainlog.dto.DietRecordDTO;
import org.examplexxx.trainlog.vo.DietRecordVO;

import java.time.LocalDate;
import java.util.List;

public interface DietService {

    List<DietRecordVO> list(LocalDate date);

    DietRecordVO create(DietRecordDTO dto);

    /** 异步任务里没有登录上下文，用消息里的 userId 写入 */
    DietRecordVO createForUser(Long userId, DietRecordDTO dto);

    DietRecordVO update(Long id, DietRecordDTO dto);

    void delete(Long id);
}
