package org.examplexxx.trainlog.service;

import lombok.Data;
import org.examplexxx.trainlog.context.BaseContext;
import org.examplexxx.trainlog.entity.DietRecognitionTask;
import org.examplexxx.trainlog.exception.BizException;
import org.examplexxx.trainlog.mapper.DietRecognitionTaskMapper;
import org.examplexxx.trainlog.vo.DietRecognitionTaskVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * 查询图片识别任务状态，供前端轮询。
 */
@Data
@Service
public class DietRecognitionTaskService {

    @Autowired
    private DietRecognitionTaskMapper taskMapper;


    public DietRecognitionTaskVO get(String taskId) {
        Long userId = BaseContext.getCurrentId();
        if (userId == null) {
            throw new BizException(401, "请先登录");
        }
        DietRecognitionTask task = taskMapper.findByTaskId(taskId);
        if (task == null || !userId.equals(task.getUserId())) {
            throw new BizException(404, "识别任务不存在");
        }
        return DietRecognitionTaskVO.from(task);
    }
}
