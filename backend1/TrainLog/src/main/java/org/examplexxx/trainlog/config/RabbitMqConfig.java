package org.examplexxx.trainlog.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.ExchangeBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.annotation.EnableRabbit;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RabbitMQ 基础配置。
 *
 * <p>启动后会自动声明：
 * <ul>
 *   <li>topic 交换机 trainlog.topic</li>
 *   <li>队列 trainlog.default.queue</li>
 *   <li>绑定关系 trainlog.default.#</li>
 * </ul>
 *
 * <p>业务代码里发消息：
 * <pre>{@code
 * rabbitTemplate.convertAndSend(
 *         RabbitMqConfig.EXCHANGE,
 *         "trainlog.default.diet",
 *         payload);
 * }</pre>
 *
 * <p>消费消息：
 * <pre>{@code
 * @RabbitListener(queues = RabbitMqConfig.QUEUE)
 * public void onMessage(DietMessage message) {
 *     ...
 * }
 * }</pre>
 */
@Configuration
@EnableRabbit
@Slf4j
public class RabbitMqConfig {

    /** 统一 topic 交换机 */
    public static final String EXCHANGE = "trainlog.topic";

    /** 默认队列，业务多了可以按模块再拆队列 */
    public static final String QUEUE = "trainlog.default.queue";

    /** 默认路由键，主要用来演示/兜底 */
    public static final String ROUTING_KEY = "trainlog.default.#";

    /** 饮食图片异步识别队列 */
    public static final String DIET_RECOGNITION_QUEUE = "trainlog.diet.recognition.queue";

    /** 饮食图片异步识别路由键 */
    public static final String DIET_RECOGNITION_ROUTING_KEY = "trainlog.diet.recognition";

    @Bean
    public TopicExchange trainlogExchange() {
        return ExchangeBuilder.topicExchange(EXCHANGE)
                .durable(true)
                .build();
    }

    @Bean
    public Queue trainlogQueue() {
        return QueueBuilder.durable(QUEUE).build();
    }

    @Bean
    public Binding trainlogBinding(Queue trainlogQueue, TopicExchange trainlogExchange) {
        return BindingBuilder.bind(trainlogQueue)
                .to(trainlogExchange)
                .with(ROUTING_KEY);
    }

    @Bean
    public Queue dietRecognitionQueue() {
        return QueueBuilder.durable(DIET_RECOGNITION_QUEUE).build();
    }

    @Bean
    public Binding dietRecognitionBinding(Queue dietRecognitionQueue,
                                          TopicExchange trainlogExchange) {
        return BindingBuilder.bind(dietRecognitionQueue)
                .to(trainlogExchange)
                .with(DIET_RECOGNITION_ROUTING_KEY);
    }

    /**
     * 消息体按 JSON 序列化，发 Map/POJO 都能直接用。
     * Spring Boot 会把这个 MessageConverter 同时应用到 RabbitTemplate 和监听容器。
     *
     * <p>注意：项目用的是 Jackson 2（com.fasterxml），Spring Boot 4 的 MVC 默认是 Jackson 3，
     * 所以这里自己 new ObjectMapper()，不依赖容器里的 ObjectMapper Bean。
     */
    @Bean
    public MessageConverter rabbitMessageConverter() {
        return new Jackson2JsonMessageConverter(new ObjectMapper());
    }

    /**
     * 启动后主动声明一次交换机/队列，避免必须先发一条消息才能在 RabbitMQ 控制台看到。
     * RabbitMQ 没启动时只记警告，不影响项目启动。
     */
    @Bean
    public RabbitAdmin rabbitAdmin(ConnectionFactory connectionFactory) {
        RabbitAdmin admin = new RabbitAdmin(connectionFactory);
        admin.setAutoStartup(true);
        return admin;
    }

    @Bean
    public ApplicationRunner rabbitAdminInitializer(RabbitAdmin rabbitAdmin) {
        return args -> {
            try {
                rabbitAdmin.initialize();
                log.info("RabbitMQ 交换机/队列声明完成：exchange={}, queue={}", EXCHANGE, QUEUE);
            } catch (Exception e) {
                log.warn("RabbitMQ 初始化失败（不影响项目启动）：{}", e.getMessage());
            }
        };
    }
}
