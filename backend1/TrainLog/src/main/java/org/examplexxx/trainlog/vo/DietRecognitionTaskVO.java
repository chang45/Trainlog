package org.examplexxx.trainlog.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.examplexxx.trainlog.entity.DietRecognitionTask;

/**
 * 图片识别任务状态，前端轮询用。
 */
@Data
@AllArgsConstructor
public class DietRecognitionTaskVO {

    private String taskId;

    private String status;

    private Long recordId;

    private String errorMessage;

    private String mealType;

    public static DietRecognitionTaskVO from(DietRecognitionTask task) {
        return new DietRecognitionTaskVO(task.getTaskId(), task.getStatus(),
                task.getRecordId(), task.getErrorMessage(), task.getMealType());
    }
}
