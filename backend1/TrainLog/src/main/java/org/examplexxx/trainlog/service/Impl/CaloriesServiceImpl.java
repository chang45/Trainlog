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
import org.examplexxx.trainlog.constant.TrainingType;
import org.examplexxx.trainlog.context.BaseContext;
import org.examplexxx.trainlog.dto.CaloriesDTO;
import org.examplexxx.trainlog.entity.SysUser;
import org.examplexxx.trainlog.exception.BizException;
import org.examplexxx.trainlog.mapper.AuthMapper;
import org.examplexxx.trainlog.properties.ArkProperties;
import org.examplexxx.trainlog.service.CaloriesService;
import org.examplexxx.trainlog.vo.CaloriesVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;

/**
 * 用火山引擎方舟（Ark）大模型估算训练消耗热量。
 *
 * <p>主链路：把用户身高体重 + 训练参数交给大模型，要求它只返回 JSON；
 * 模型不可用（没配 key、超时、返回格式异常）时回退到 MET 公式，
 * 保证接口始终有结果，但会用 {@link CaloriesVO#getSource()} 标明来源。
 */
@Service
@Data
@Slf4j
public class CaloriesServiceImpl implements CaloriesService {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final int MAX_CALORIES = 5000;

    @Autowired
    private ArkService arkService;
    @Autowired
    private ArkProperties arkProperties;
    @Autowired
    private AuthMapper authMapper;

    @Override
    public CaloriesVO estimate(CaloriesDTO request) {
        Long userId = currentUserId();
        TrainingType type = validate(request);

        SysUser user = authMapper.findById(userId);
        BigDecimal height = user == null ? null : user.getHeightCm();
        BigDecimal weight = user == null ? null : user.getWeightKg();

        if (StringUtils.hasText(arkProperties.getApiKey())) {
            try {
                String answer = callArk(request, type, height, weight, user);
                CaloriesVO parsed = parseAnswer(answer);
                if (parsed != null) {
                    return parsed;
                }
                log.warn("Ark 返回内容无法解析成热量 JSON：{}", abbreviate(answer));
            } catch (Exception e) {
                log.warn("调用 Ark 估算热量失败，回退公式：{}", e.getMessage());
            }
        } else {
            log.debug("ark.api-key 未配置，使用 MET 公式估算热量");
        }

        return formula(request, type, weight);
    }

    // ==================== 调用大模型 ====================

    private String callArk(CaloriesDTO request, TrainingType type,
                           BigDecimal height, BigDecimal weight, SysUser user) {
        CreateResponsesRequest arkRequest = CreateResponsesRequest.builder()
                .model(arkProperties.getModel())
                .instructions("你是专业运动科学教练，擅长估算训练能量消耗。你只输出 JSON，不输出任何解释性文字。")
                .temperature(0.2)
                .maxOutputTokens(300L)
                .input(ResponsesInput.builder()
                        .addListItem(ItemEasyMessage.builder()
                                .role(ResponsesConstants.MESSAGE_ROLE_USER)
                                .content(MessageContent.builder()
                                        .addListItem(InputContentItemText.builder()
                                                .text(buildPrompt(request, type, height, weight, user))
                                                .build())
                                        .build())
                                .build())
                        .build())
                .build();

        ResponseObject response = arkService.createResponse(arkRequest);
        return extractText(response);
    }

    private String buildPrompt(CaloriesDTO request, TrainingType type,
                               BigDecimal height, BigDecimal weight, SysUser user) {
        StringBuilder sb = new StringBuilder();
        sb.append("请根据以下训练数据估算消耗的热量。\n");
        sb.append("用户：身高 ").append(height == null ? "未知" : height + " cm")
                .append("，体重 ").append(weight == null ? "未知" : weight + " kg").append("。\n");
        if (user != null && user.getAge() != null) {
            sb.append("年龄：").append(user.getAge()).append(" 岁。\n");
        }
        if (user != null && user.getGender() != null) {
            sb.append("性别：").append(genderLabel(user.getGender())).append("。\n");
        }
        sb.append("训练项目：").append(type.getLabel())
                .append("（").append("STRENGTH".equals(type.getCategory()) ? "力量训练" : "有氧训练")
                .append("）。\n");
        sb.append("训练时长：").append(request.getDurationMinutes()).append(" 分钟。\n");
        if (request.getDistanceKm() != null) {
            sb.append("距离：").append(request.getDistanceKm()).append(" 公里。\n");
        }
        if (request.getSets() != null || request.getReps() != null || request.getWeightKg() != null) {
            sb.append("力量参数：");
            if (request.getSets() != null) {
                sb.append(request.getSets()).append(" 组");
            }
            if (request.getReps() != null) {
                sb.append(" × ").append(request.getReps()).append(" 次");
            }
            if (request.getWeightKg() != null) {
                sb.append("，负重 ").append(request.getWeightKg()).append(" kg");
            }
            sb.append("。\n");
        }
        if (request.getIntensity() != null) {
            sb.append("主观强度 RPE（用户自己感受，1-10）：")
                    .append(request.getIntensity()).append("/10。\n");
        }
        sb.append("估算时请结合体重、项目、时长与强度，给出一个合理整数。\n");
        sb.append("只返回 JSON，不要 markdown，不要多余文字。格式：")
                .append("{\"calories\": 整数, \"reason\": \"不超过 40 字的中文说明\"}");
        return sb.toString();
    }

    /** 从 Responses API 的 output 里取第一段文本 */
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

    private CaloriesVO parseAnswer(String answer) {
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
            JsonNode caloriesNode = node.get("calories");
            if (caloriesNode == null || !caloriesNode.isNumber()) {
                return null;
            }
            int calories = caloriesNode.asInt();
            if (calories <= 0) {
                return null;
            }
            String reason = node.hasNonNull("reason")
                    ? node.get("reason").asText()
                    : "大模型估算";
            return new CaloriesVO(Math.min(calories, MAX_CALORIES), "AI", reason);
        } catch (Exception e) {
            return null;
        }
    }

    // ==================== 兜底公式 ====================

    private CaloriesVO formula(CaloriesDTO request, TrainingType type, BigDecimal weight) {
        double kilograms = weight == null ? 65.0 : weight.doubleValue();
        double hours = request.getDurationMinutes() / 60.0;
        int rpe = request.getIntensity() == null ? 6 : request.getIntensity();
        double factor = 0.8 + rpe * 0.04; // RPE 1->0.84, 10->1.2

        int calories = (int) Math.round(metOf(type) * kilograms * hours * factor);
        calories = Math.max(1, Math.min(calories, MAX_CALORIES));
        String explanation = String.format("按 MET 公式估算（%.1f kg，MET %.1f）", kilograms, metOf(type));
        return new CaloriesVO(calories, "FORMULA", explanation);
    }

    private double metOf(TrainingType type) {
        return switch (type) {
            case CHEST, SHOULDER, ABS -> 5.0;
            case BACK, HIPS -> 5.5;
            case LEG -> 6.0;
            case INCLINE -> 6.0;
            case CYCLING -> 7.5;
            case RUNNING -> 9.8;
        };
    }

    // ==================== 校验 ====================

    private TrainingType validate(CaloriesDTO request) {
        if (request == null) {
            throw new BizException(400, "请求体不能为空");
        }
        TrainingType type = TrainingType.find(request.getType())
                .orElseThrow(() -> new BizException(400, "未知的训练类型"));
        if (request.getDurationMinutes() == null || request.getDurationMinutes() < 1) {
            throw new BizException(400, "训练时长至少 1 分钟");
        }
        if (request.getDurationMinutes() > 600) {
            throw new BizException(400, "训练时长不能超过 600 分钟");
        }
        if (request.getIntensity() != null
                && (request.getIntensity() < 1 || request.getIntensity() > 10)) {
            throw new BizException(400, "主观强度需在 1-10 之间");
        }
        if (request.getWeightKg() != null
                && request.getWeightKg().compareTo(BigDecimal.ZERO) < 0) {
            throw new BizException(400, "负重不能为负数");
        }
        return type;
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

    private String genderLabel(String gender) {
        return "FEMALE".equals(gender) ? "女" : "男";
    }
}
