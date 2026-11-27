package org.examplexxx.trainlog.service.Impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.volcengine.ark.runtime.model.responses.common.ResponsesThinking;
import com.volcengine.ark.runtime.model.responses.constant.ResponsesConstants;
import com.volcengine.ark.runtime.model.responses.content.InputContentItemImage;
import com.volcengine.ark.runtime.model.responses.content.InputContentItemText;
import com.volcengine.ark.runtime.model.responses.content.OutputContentItem;
import com.volcengine.ark.runtime.model.responses.content.OutputContentItemText;
import com.volcengine.ark.runtime.model.responses.item.BaseItem;
import com.volcengine.ark.runtime.model.responses.item.ItemEasyMessage;
import com.volcengine.ark.runtime.model.responses.item.ItemOutputMessage;
import com.volcengine.ark.runtime.model.responses.item.MessageContent;
import com.volcengine.ark.runtime.model.responses.request.CreateResponsesRequest;
import com.volcengine.ark.runtime.model.responses.request.ResponsesInput;
import com.volcengine.ark.runtime.model.responses.response.ResponseObject;
import com.volcengine.ark.runtime.service.ArkService;
import lombok.extern.slf4j.Slf4j;
import org.examplexxx.trainlog.dto.DietRecognitionDTO;
import org.examplexxx.trainlog.exception.BizException;
import org.examplexxx.trainlog.properties.ArkProperties;
import org.examplexxx.trainlog.service.DietRecognitionService;
import org.examplexxx.trainlog.vo.DietRecognitionVO;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Locale;

/**
 * 把饮食图片交给火山方舟视觉模型，识别食物并估算热量/蛋白质/碳水/脂肪。
 */
@Service
@Slf4j
public class DietRecognitionServiceImpl implements DietRecognitionService {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final ArkService arkService;
    private final ArkProperties arkProperties;

    public DietRecognitionServiceImpl(ArkService arkService, ArkProperties arkProperties) {
        this.arkService = arkService;
        this.arkProperties = arkProperties;
    }

    @Override
    public DietRecognitionVO recognize(DietRecognitionDTO dto) {
        log.info("饮食识别请求：mealType={}, imageLength={}",
                dto == null ? null : dto.getMealType(),
                dto == null || dto.getImageBase64() == null ? 0 : dto.getImageBase64().length());
        if (dto == null || !StringUtils.hasText(dto.getImageBase64())) {
            log.warn("饮食识别失败：图片为空");
            throw new BizException(400, "请上传饮食图片");
        }
        String image = dto.getImageBase64();
        if (!image.startsWith("data:image/")) {
            log.warn("饮食识别失败：不是 data:image 格式，前缀={}", image.substring(0, Math.min(30, image.length())));
            throw new BizException(400, "图片格式不正确");
        }
        if (image.length() > 8_000_000) {
            log.warn("饮食识别失败：图片过大，length={}", image.length());
            throw new BizException(400, "图片太大，请压缩后再试");
        }
        if (!StringUtils.hasText(arkProperties.getApiKey())) {
            log.warn("饮食识别失败：ark.api-key 未配置");
            throw new BizException(503, "AI 未配置，暂时无法识别");
        }

        try {
            log.info("调用 Ark 视觉模型：model={}, baseUrl={}", arkProperties.getModel(), arkProperties.getBaseUrl());
            String answer = callArk(image, dto.getMealType());
            log.info("Ark 返回长度：{}", answer == null ? 0 : answer.length());
            DietRecognitionVO result = parseAnswer(answer);
            if (result == null) {
                log.warn("饮食识别返回无法解析：{}", abbreviate(answer));
                throw new BizException(502, "AI 识别失败，请换一张清晰的图片");
            }
            return result;
        } catch (BizException e) {
            throw e;
        } catch (Exception e) {
            log.warn("饮食图片识别失败：{} - {}", e.getClass().getSimpleName(), e.getMessage(), e);
            throw new BizException(502, "AI 识别失败，请稍后重试");
        }
    }

    private String callArk(String imageDataUrl, String mealType) {
        String prompt = "你是营养师。请识别这张图片里的食物，估算这一餐的总热量和三大营养素。\n"
                + "餐次：" + mealLabel(mealType) + "。\n"
                + "只返回 JSON，不要 markdown，不要多余文字。格式："
                + "{\"foodName\":\"食物名\",\"calories\":整数,\"protein\":数字,"
                + "\"carbs\":数字,\"fat\":数字,\"reason\":\"不超过 40 字的中文说明\"}\n"
                + "单位：热量千卡，蛋白质/碳水/脂肪克。";

        CreateResponsesRequest request = CreateResponsesRequest.builder()
                .model(arkProperties.getModel())
                .thinking(ResponsesThinking.builder().type("disabled").build())
                .instructions("你是营养师，只输出 JSON。")
                .temperature(0.2)
                .maxOutputTokens(1000L)
                .input(ResponsesInput.builder()
                        .addListItem(ItemEasyMessage.builder()
                                .role(ResponsesConstants.MESSAGE_ROLE_USER)
                                .content(MessageContent.builder()
                                        .addListItem(InputContentItemImage.builder()
                                                .imageUrl(imageDataUrl)
                                                .build())
                                        .addListItem(InputContentItemText.builder()
                                                .text(prompt)
                                                .build())
                                        .build())
                                .build())
                        .build())
                .build();

        ResponseObject response = arkService.createResponse(request);
        log.info("Ark 响应：status={}, error={}, outputSize={}",
                response == null ? null : response.getStatus(),
                response == null || response.getError() == null
                        ? null
                        : response.getError().getCode() + ":" + response.getError().getMessage(),
                response == null || response.getOutput() == null ? -1 : response.getOutput().size());
        return extractText(response);
    }

    private DietRecognitionVO parseAnswer(String answer) {
        if (!StringUtils.hasText(answer)) {
            return null;
        }
        String json = answer.replace("```json", "").replace("```", "").trim();
        int start = json.indexOf('{');
        int end = json.lastIndexOf('}');
        if (start < 0 || end <= start) {
            return null;
        }
        try {
            JsonNode node = MAPPER.readTree(json.substring(start, end + 1));
            String foodName = node.hasNonNull("foodName") ? node.get("foodName").asText() : null;
            if (!StringUtils.hasText(foodName)) {
                return null;
            }
            Integer calories = intValue(node, "calories");
            BigDecimal protein = decimal(node, "protein");
            BigDecimal carbs = decimal(node, "carbs");
            BigDecimal fat = decimal(node, "fat");
            if (calories == null || calories < 0 || calories > 5000
                    || protein == null || protein.compareTo(BigDecimal.ZERO) < 0
                    || protein.compareTo(BigDecimal.valueOf(500)) > 0
                    || carbs == null || carbs.compareTo(BigDecimal.ZERO) < 0
                    || carbs.compareTo(BigDecimal.valueOf(500)) > 0
                    || fat == null || fat.compareTo(BigDecimal.ZERO) < 0
                    || fat.compareTo(BigDecimal.valueOf(500)) > 0) {
                return null;
            }
            String reason = node.hasNonNull("reason")
                    ? node.get("reason").asText()
                    : "AI 估算";
            return new DietRecognitionVO(foodName, calories, protein, carbs, fat, reason);
        } catch (Exception e) {
            return null;
        }
    }

    /** 兼容模型把数字写成 420 或 "420" 两种情况 */
    private Integer intValue(JsonNode node, String field) {
        JsonNode value = node.get(field);
        if (value == null || value.isNull()) {
            return null;
        }
        if (value.isNumber()) {
            return value.asInt();
        }
        if (value.isTextual()) {
            try {
                return new BigDecimal(value.asText().trim()).intValue();
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
    }

    private BigDecimal decimal(JsonNode node, String field) {
        JsonNode value = node.get(field);
        if (value == null || value.isNull()) {
            return null;
        }
        BigDecimal number = null;
        if (value.isNumber()) {
            number = value.decimalValue();
        } else if (value.isTextual()) {
            try {
                number = new BigDecimal(value.asText().trim());
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return number == null ? null : number.setScale(1, RoundingMode.HALF_UP);
    }

    private String extractText(ResponseObject response) {
        if (response == null || response.getOutput() == null) {
            return null;
        }
        for (BaseItem item : response.getOutput()) {
            if (item instanceof ItemOutputMessage message && message.getContent() != null) {
                for (OutputContentItem content : message.getContent()) {
                    if (content instanceof OutputContentItemText text && text.getText() != null) {
                        return text.getText();
                    }
                }
            }
        }
        return null;
    }

    private String abbreviate(String text) {
        if (text == null) {
            return "";
        }
        return text.length() <= 300 ? text : text.substring(0, 300) + "...";
    }

    private String mealLabel(String mealType) {
        if (mealType == null) {
            return "未指定";
        }
        return switch (mealType.trim().toUpperCase(Locale.ROOT)) {
            case "BREAKFAST" -> "早餐";
            case "LUNCH" -> "午餐";
            case "DINNER" -> "晚餐";
            default -> "未指定";
        };
    }
}
