package org.examplexxx.trainlog.mq;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.examplexxx.trainlog.config.RabbitMqConfig;
import org.examplexxx.trainlog.context.BaseContext;
import org.examplexxx.trainlog.dto.DietRecognitionDTO;
import org.examplexxx.trainlog.dto.DietRecognitionMessageDTO;
import org.examplexxx.trainlog.entity.DietRecognitionTask;
import org.examplexxx.trainlog.exception.BizException;
import org.examplexxx.trainlog.mapper.DietRecognitionTaskMapper;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 饮食图片识别生产者：只负责建任务 + 发消息，不调 AI。
 */
@Data
@Service
@Slf4j
public class DietRecognitionProducer {

    @Autowired
    private RabbitTemplate rabbitTemplate;
    @Autowired
    private DietRecognitionTaskMapper taskMapper;

    public String submit(DietRecognitionDTO dto) {
        Long userId = currentUserId();
        if (dto == null || !StringUtils.hasText(dto.getImageBase64())) {
            throw new BizException(400, "请上传饮食图片");
        }
        String image = dto.getImageBase64();
        if (!image.startsWith("data:image/")) {
            throw new BizException(400, "图片格式不正确");
        }
        if (image.length() > 8_000_000) {
            throw new BizException(400, "图片太大，请压缩后再试");
        }

        String taskId = UUID.randomUUID().toString();
        LocalDate eatenDate = dto.getEatenDate() == null ? LocalDate.now() : dto.getEatenDate();
        LocalDateTime now = LocalDateTime.now();

        DietRecognitionTask task = new DietRecognitionTask();
        task.setTaskId(taskId);
        task.setUserId(userId);
        task.setMealType(dto.getMealType());
        task.setStatus("PENDING");
        task.setCreatedAt(now);
        task.setUpdatedAt(now);
        taskMapper.insert(task);

        DietRecognitionMessageDTO message = new DietRecognitionMessageDTO(
                taskId, userId, dto.getMealType(), image, eatenDate.toString());

        try {
            rabbitTemplate.convertAndSend(
                    RabbitMqConfig.EXCHANGE,
                    RabbitMqConfig.DIET_RECOGNITION_ROUTING_KEY,
                    message);
        } catch (Exception e) {
            log.warn("饮食识别消息发送失败：{}", e.getMessage());
            task.setStatus("FAILED");
            task.setErrorMessage("消息发送失败，请稍后重试");
            task.setUpdatedAt(LocalDateTime.now());
            taskMapper.update(task);
            throw new BizException(503, "识别任务提交失败，请稍后重试");
        }

        log.info("饮食识别任务已提交：taskId={}, userId={}, mealType={}",
                taskId, userId, dto.getMealType());
        return taskId;
    }

    private Long currentUserId() {
        Long userId = BaseContext.getCurrentId();
        if (userId == null) {
            throw new BizException(401, "请先登录");
        }
        return userId;
    }
}
