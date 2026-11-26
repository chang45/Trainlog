package org.examplexxx.trainlog.service.Impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.volcengine.ark.runtime.model.responses.constant.ResponsesConstants;
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
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.examplexxx.trainlog.context.BaseContext;
import org.examplexxx.trainlog.dto.DailyCaloriesRequest;
import org.examplexxx.trainlog.entity.SysUser;
import org.examplexxx.trainlog.exception.BizException;
import org.examplexxx.trainlog.mapper.AuthMapper;
import org.examplexxx.trainlog.properties.ArkProperties;
import org.examplexxx.trainlog.service.DailyCaloriesService;
import org.examplexxx.trainlog.vo.DailyCaloriesVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;

/**
 * 用身高体重推算每天的基础代谢（BMR）和总消耗（TDEE）。
 *
 * <p>优先让火山引擎方舟大模型估算；没配 key 或调用失败时，
 * 回退到 Mifflin-St Jeor 公式（按 30 岁、轻度活动估算）。
 */
@Data
@Service
@Slf4j
public class DailyCaloriesServiceImpl implements DailyCaloriesService {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Autowired
    private ArkService arkService;
    @Autowired
    private ArkProperties arkProperties;
    @Autowired
    private AuthMapper authMapper;


    @Override
    public DailyCaloriesVO estimate(DailyCaloriesRequest request) {
        Long userId = currentUserId();
        SysUser user = authMapper.findById(userId);

        BigDecimal height = request != null && request.getHeightCm() != null
                ? request.getHeightCm()
                : (user == null ? null : user.getHeightCm());
        BigDecimal weight = request != null && request.getWeightKg() != null
                ? request.getWeightKg()
                : (user == null ? null : user.getWeightKg());

        Integer age = request != null && request.getAge() != null
                ? request.getAge()
                : (user == null ? null : user.getAge());
        String gender = request != null && request.getGender() != null
                ? request.getGender()
                : (user == null ? null : user.getGender());

        validate(height, weight, age, gender);

        if (StringUtils.hasText(arkProperties.getApiKey())) {
            try {
                String answer = callArk(height, weight, age, gender);
                DailyCaloriesVO parsed = parseAnswer(answer);
                if (parsed != null) {
                    return parsed;
                }
                log.warn("Ark 返回内容无法解析成每日热量 JSON：{}", abbreviate(answer));
            } catch (Exception e) {
                log.warn("调用 Ark 推算每日热量失败，回退公式：{}", e.getMessage());
            }
        } else {
            log.debug("ark.api-key 未配置，使用 Mifflin 公式推算每日热量");
        }

        return formula(height, weight, age, gender);
    }

    // ==================== 调用大模型 ====================

    private String callArk(BigDecimal height, BigDecimal weight, Integer age, String gender) {
        CreateResponsesRequest arkRequest = CreateResponsesRequest.builder()
                .model(arkProperties.getModel())
                .instructions("你是运动营养师，只输出 JSON，不输出任何解释性文字。")
                .temperature(0.2)
                .maxOutputTokens(300L)
                .input(ResponsesInput.builder()
                        .addListItem(ItemEasyMessage.builder()
                                .role(ResponsesConstants.MESSAGE_ROLE_USER)
                                .content(MessageContent.builder()
                                        .addListItem(InputContentItemText.builder()
                                                .text(buildPrompt(height, weight, age, gender))
                                                .build())
                                        .build())
                                .build())
                        .build())
                .build();

        ResponseObject response = arkService.createResponse(arkRequest);
        return extractText(response);
    }

    private String buildPrompt(BigDecimal height, BigDecimal weight, Integer age, String gender) {
        return "用户：" + age + " 岁，" + genderLabel(gender)
                + "，身高 " + height + " cm，体重 " + weight + " kg。\n"
                + "请按轻度活动（每周 1-3 次训练）估算每日总消耗（TDEE），"
                + "并给出基础代谢率（BMR）。\n"
                + "只返回 JSON，不要 markdown，不要多余文字。格式："
                + "{\"bmr\": 整数, \"tdee\": 整数, \"reason\": \"不超过 40 字的中文说明\"}";
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

    private DailyCaloriesVO parseAnswer(String answer) {
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
            JsonNode bmrNode = node.get("bmr");
            JsonNode tdeeNode = node.get("tdee");
            if (bmrNode == null || !bmrNode.isNumber() || tdeeNode == null || !tdeeNode.isNumber()) {
                return null;
            }
            int bmr = bmrNode.asInt();
            int tdee = tdeeNode.asInt();
            if (bmr < 500 || bmr > 5000 || tdee < 800 || tdee > 10000 || tdee < bmr) {
                return null;
            }
            String reason = node.hasNonNull("reason")
                    ? node.get("reason").asText()
                    : "大模型推算";
            return new DailyCaloriesVO(bmr, tdee, "AI", reason);
        } catch (Exception e) {
            return null;
        }
    }

    // ==================== 兜底公式 ====================

    private DailyCaloriesVO formula(BigDecimal height, BigDecimal weight,
                                    Integer age, String gender) {
        double h = height.doubleValue();
        double w = weight.doubleValue();
        double genderConstant = switch (gender) {
            case "MALE" -> 5;
            case "FEMALE" -> -161;
            default -> -78;
        };
        // Mifflin-St Jeor
        double bmrValue = 10 * w + 6.25 * h - 5 * age + genderConstant;
        int bmr = (int) Math.round(bmrValue);
        int tdee = (int) Math.round(bmrValue * 1.375);
        bmr = Math.max(500, Math.min(bmr, 5000));
        tdee = Math.max(800, Math.min(tdee, 10000));
        String explanation = String.format(
                "按 Mifflin-St Jeor 公式估算（%d 岁%s，%.1f cm / %.1f kg，按轻度活动）",
                age, genderLabel(gender), h, w);
        return new DailyCaloriesVO(bmr, tdee, "FORMULA", explanation);
    }

    private String genderLabel(String gender) {
        return "FEMALE".equals(gender) ? "女" : "男";
    }

    // ==================== 校验 ====================

    private void validate(BigDecimal height, BigDecimal weight, Integer age, String gender) {
        if (height == null || height.compareTo(BigDecimal.valueOf(50)) < 0
                || height.compareTo(BigDecimal.valueOf(250)) > 0) {
            throw new BizException(400, "请先填写有效身高（50-250 厘米）");
        }
        if (weight == null || weight.compareTo(BigDecimal.valueOf(20)) < 0
                || weight.compareTo(BigDecimal.valueOf(300)) > 0) {
            throw new BizException(400, "请先填写有效体重（20-300 公斤）");
        }
        if (age == null || age < 10 || age > 100) {
            throw new BizException(400, "请先填写有效年龄（10-100 岁）");
        }
        if (!"MALE".equals(gender) && !"FEMALE".equals(gender)) {
            throw new BizException(400, "请先选择性别");
        }
    }

    private Long currentUserId() {
        Long userId = BaseContext.getCurrentId();
        if (userId == null) {
            throw new BizException(401, "请先登录");
        }
        return userId;
    }

    private String abbreviate(String text) {
        if (text == null) {
            return "";
        }
        return text.length() <= 120 ? text : text.substring(0, 120) + "...";
    }
}
