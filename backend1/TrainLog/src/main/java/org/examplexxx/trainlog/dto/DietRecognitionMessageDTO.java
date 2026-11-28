package org.examplexxx.trainlog.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 图片识别任务消息，生产者发到 RabbitMQ，消费者异步识别。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DietRecognitionMessageDTO {

    private String taskId;

    private Long userId;

    private String mealType;

    /** data:image/jpeg;base64,... */
    private String imageBase64;

    /** ISO 日期，例如 2026-11-28；消息里不用 LocalDate，避免序列化依赖 */
    private String eatenDate;
}
