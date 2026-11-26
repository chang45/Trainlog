package org.examplexxx.trainlog.config;

import com.volcengine.ark.runtime.service.ArkService;
import lombok.extern.slf4j.Slf4j;
import org.examplexxx.trainlog.properties.ArkProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

/**
 * 火山引擎方舟（Ark）客户端配置。
 *
 * <p>会创建一个全局 {@link ArkService} Bean，容器关闭时自动调用 {@code shutdownExecutor()}
 * 释放内部线程池。
 *
 * <p>注意：ArkService 内部有自己的 OkHttp 连接池和线程池，不要每次请求都 new，
 * 统一注入这个 Bean 使用。
 */
@Configuration
@Slf4j
public class ArkConfig {

    @Bean(destroyMethod = "shutdownExecutor")
    public ArkService arkService(ArkProperties properties) {
        String apiKey = properties.getApiKey() == null ? "" : properties.getApiKey().trim();
        if (apiKey.isEmpty()) {
            log.warn("ark.api-key 未配置，ArkService 可以创建但调用会失败，请设置环境变量 ARK_API_KEY");
        }

        return ArkService.builder()
                .apiKey(apiKey)
                .baseUrl(properties.getBaseUrl())
                .connectTimeout(Duration.ofSeconds(properties.getConnectTimeoutSeconds()))
                .timeout(Duration.ofSeconds(properties.getTimeoutSeconds()))
                .retryTimes(properties.getRetryTimes())
                .build();
    }
}
