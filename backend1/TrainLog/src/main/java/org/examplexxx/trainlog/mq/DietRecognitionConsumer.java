package org.examplexxx.trainlog.mq;

import lombok.extern.slf4j.Slf4j;
import org.examplexxx.trainlog.config.RabbitMqConfig;
import org.examplexxx.trainlog.dto.DietRecognitionDTO;
import org.examplexxx.trainlog.dto.DietRecognitionMessageDTO;
import org.examplexxx.trainlog.dto.DietRecordDTO;
import org.examplexxx.trainlog.entity.DietRecognitionTask;
import org.examplexxx.trainlog.mapper.DietRecognitionTaskMapper;
import org.examplexxx.trainlog.service.DietRecognitionService;
import org.examplexxx.trainlog.service.DietService;
import org.examplexxx.trainlog.vo.DietRecognitionVO;
import org.examplexxx.trainlog.vo.DietRecordVO;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 饮食图片识别消费者：
 * 收到消息后调 AI，识别成功直接写入 diet_record，用户可以在前端再编辑。
 */
@Component
@Slf4j
public class DietRecognitionConsumer {

    @Autowired
    private DietRecognitionTaskMapper taskMapper;
    @Autowired
    private DietRecognitionService recognitionService;
    @Autowired
    private DietService dietService;


    @RabbitListener(queues = RabbitMqConfig.DIET_RECOGNITION_QUEUE)
    public void handle(DietRecognitionMessageDTO message) {
        if (message == null || message.getTaskId() == null) {
            return;
        }
        DietRecognitionTask task = taskMapper.findByTaskId(message.getTaskId());
        if (task == null) {
            log.warn("识别任务不存在，忽略：{}", message.getTaskId());
            return;
        }
        if ("SUCCESS".equals(task.getStatus())) {
            return;
        }

        task.setStatus("PROCESSING");
        task.setUpdatedAt(LocalDateTime.now());
        taskMapper.update(task);

        try {
            DietRecognitionDTO recognitionDTO = new DietRecognitionDTO();
            recognitionDTO.setMealType(message.getMealType());
            recognitionDTO.setImageBase64(message.getImageBase64());
            DietRecognitionVO result = recognitionService.recognize(recognitionDTO);

            DietRecordDTO recordDTO = new DietRecordDTO();
            recordDTO.setMealType(message.getMealType());
            recordDTO.setFoodName(result.getFoodName());
            recordDTO.setCalories(result.getCalories());
            recordDTO.setProtein(result.getProtein());
            recordDTO.setCarbs(result.getCarbs());
            recordDTO.setFat(result.getFat());
            recordDTO.setEatenDate(parseDate(message.getEatenDate()));

            DietRecordVO record = dietService.createForUser(message.getUserId(), recordDTO);

            task.setStatus("SUCCESS");
            task.setRecordId(record.getId());
            task.setErrorMessage(null);
            log.info("饮食识别成功：taskId={}, recordId={}, food={}",
                    task.getTaskId(), record.getId(), result.getFoodName());
        } catch (Exception e) {
            task.setStatus("FAILED");
            task.setErrorMessage(abbreviate(e.getMessage()));
            log.warn("饮食识别失败：taskId={}, error={}", task.getTaskId(), e.getMessage(), e);
        } finally {
            task.setUpdatedAt(LocalDateTime.now());
            taskMapper.update(task);
        }
    }

    private LocalDate parseDate(String value) {
        try {
            return value == null ? LocalDate.now() : LocalDate.parse(value);
        } catch (Exception e) {
            return LocalDate.now();
        }
    }

    private String abbreviate(String text) {
        if (text == null) {
            return "识别失败";
        }
        return text.length() <= 500 ? text : text.substring(0, 500);
    }
}
