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
import lombok.extern.slf4j.Slf4j;
import org.examplexxx.trainlog.context.BaseContext;
import org.examplexxx.trainlog.dto.ProteinDTO;
import org.examplexxx.trainlog.entity.SysUser;
import org.examplexxx.trainlog.exception.BizException;
import org.examplexxx.trainlog.mapper.AuthMapper;
import org.examplexxx.trainlog.properties.ArkProperties;
import org.examplexxx.trainlog.service.ProteinService;
import org.examplexxx.trainlog.vo.ProteinVO;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.Locale;

/**
 * 用火山引擎方舟大模型推算每日蛋白质需求：
 * 正常维持 + 增肌。失败时按 0.9 / 1.8 g/kg 兜底。
 */
@Service
@Slf4j
public class ProteinServiceImpl implements ProteinService {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final ArkService arkService;
    private final ArkProperties arkProperties;
    private final AuthMapper authMapper;

    public ProteinServiceImpl(ArkService arkService,
                              ArkProperties arkProperties,
                              AuthMapper authMapper) {
        this.arkService = arkService;
        this.arkProperties = arkProperties;
        this.authMapper = authMapper;
    }

    @Override
    public ProteinVO estimate(ProteinDTO request) {
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
        if (gender != null) {
            gender = gender.trim().toUpperCase(Locale.ROOT);
        }

        validate(height, weight, age, gender);

        if (StringUtils.hasText(arkProperties.getApiKey())) {
            try {
                String answer = callArk(height, weight, age, gender);
                ProteinVO parsed = parseAnswer(answer);
                if (parsed != null) {
                    return parsed;
                }
                log.warn("Ark 返回内容无法解析成蛋白质 JSON：{}", abbreviate(answer));
            } catch (Exception e) {
                log.warn("调用 Ark 推算蛋白质失败，回退公式：{}", e.getMessage());
            }
        } else {
            log.debug("ark.api-key 未配置，使用蛋白质公式估算");
        }

        return formula(weight);
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
        StringBuilder sb = new StringBuilder("用户：");
        if (age != null) {
            sb.append(age).append(" 岁，");
        }
        if (gender != null) {
            sb.append("MALE".equals(gender) ? "男" : "女").append("，");
        }
        sb.append("体重 ").append(weight).append(" kg");
        if (height != null) {
            sb.append("，身高 ").append(height).append(" cm");
        }
        sb.append("。\n")
                .append("请给出每日蛋白质推荐量，单位克/天：\n")
                .append("1. normal：正常维持（普通活动）\n")
                .append("2. bulk：增肌（力量训练）\n")
                .append("只返回 JSON，不要 markdown，不要多余文字。格式：")
                .append("{\"normal\": 整数, \"bulk\": 整数, \"reason\": \"不超过 40 字的中文说明\"}");
        return sb.toString();
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

    private ProteinVO parseAnswer(String answer) {
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
            JsonNode normalNode = node.get("normal");
            JsonNode bulkNode = node.get("bulk");
            if (normalNode == null || !normalNode.isNumber()
                    || bulkNode == null || !bulkNode.isNumber()) {
                return null;
            }
            int normal = normalNode.asInt();
            int bulk = bulkNode.asInt();
            if (normal < 30 || normal > 400 || bulk < 30 || bulk > 400 || bulk < normal) {
                return null;
            }
            String reason = node.hasNonNull("reason")
                    ? node.get("reason").asText()
                    : "大模型推算";
            return new ProteinVO(normal, bulk, "AI", reason);
        } catch (Exception e) {
            return null;
        }
    }

    // ==================== 兜底公式 ====================

    private ProteinVO formula(BigDecimal weight) {
        double w = weight.doubleValue();
        int normal = (int) Math.round(w * 0.9);
        int bulk = (int) Math.round(w * 1.8);
        normal = Math.max(30, Math.min(normal, 400));
        bulk = Math.max(normal, Math.min(bulk, 400));
        String explanation = String.format(
                "按体重估算（正常维持 0.9 g/kg，增肌 1.8 g/kg，%.1f kg）", w);
        return new ProteinVO(normal, bulk, "FORMULA", explanation);
    }

    // ==================== 校验 ====================

    private void validate(BigDecimal height, BigDecimal weight, Integer age, String gender) {
        if (weight == null || weight.compareTo(BigDecimal.valueOf(20)) < 0
                || weight.compareTo(BigDecimal.valueOf(300)) > 0) {
            throw new BizException(400, "请先填写有效体重（20-300 公斤）");
        }
        if (height != null && (height.compareTo(BigDecimal.valueOf(50)) < 0
                || height.compareTo(BigDecimal.valueOf(250)) > 0)) {
            throw new BizException(400, "身高需在 50-250 厘米之间");
        }
        if (age != null && (age < 10 || age > 100)) {
            throw new BizException(400, "年龄需在 10-100 岁之间");
        }
        if (gender != null && !"MALE".equals(gender) && !"FEMALE".equals(gender)) {
            throw new BizException(400, "性别取值不合法");
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
