package org.examplexxx.trainlog.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 火山引擎方舟（Ark）配置。
 * 对应 application.yml 里的 ark.*，敏感项建议用环境变量注入。
 */
@Data
@Component
@ConfigurationProperties(prefix = "ark")
public class ArkProperties {

    /** API Key，建议用环境变量 ARK_API_KEY 注入，不要写死在配置文件里 */
    private String apiKey;

    /** 服务地址 */
    private String baseUrl = "https://ark.cn-beijing.volces.com/api/v3";

    /** 默认模型，业务调用时可以直接用 */
    private String model = "doubao-seed-2-1-pro-260915";

    /** 连接超时（秒） */
    private int connectTimeoutSeconds = 10;

    /** 请求超时（秒），多模态/长文本可以调大 */
    private int timeoutSeconds = 60;

    /** 失败重试次数 */
    private int retryTimes = 1;
}
