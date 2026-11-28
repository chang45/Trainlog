package org.examplexxx.trainlog.entity;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 图片识别异步任务，对应表 diet_recognition_task。
 */
@Data
public class DietRecognitionTask {

    private Long id;

    private String taskId;

    private Long userId;

    private String mealType;

    /** PENDING / PROCESSING / SUCCESS / FAILED */
    private String status;

    /** 识别成功后自动写入的饮食记录 id */
    private Long recordId;

    private String errorMessage;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
