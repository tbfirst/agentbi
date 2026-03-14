package com.tbfirst.agentbiinit.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.*;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * rabbitmq 消息队列配置类
 * 该类中的组件会被自动扫描并注册到 Spring 容器中，即不需要我们自己手动创建队列、交换机等，而是通过注解配置即可
 */
@Configuration
@Slf4j
public class RabbitConfig {

    // ==================== 正常业务配置 ====================

    /** 正常业务交换机：消息首先发到这里 */
    public static final String CHART_EXCHANGE = "chart.exchange";

    /** 正常业务队列：正常消费者监听这里，失败消息转死信 */
    public static final String CHART_QUEUE = "chart.queue";

    /** 正常业务路由键 */
    public static final String CHART_ROUTING_KEY = "chart.routing.key";

    // ==================== 死信配置（失败转移用） ====================

    /** 死信交换机：失败消息自动转到这里 */
    public static final String CHART_DLX_EXCHANGE = "chart.dlx.exchange";

    /** 死信队列：死信消费者监听这里，处理人工介入 */
    public static final String CHART_DLX_QUEUE = "chart.dlx.queue";

    /** 死信路由键 */
    public static final String CHART_DLX_ROUTING_KEY = "chart.dlx.routing.key";

    /**
     * 正常业务交换机：生产者发消息到这里
     * 这里使用 Direct交换机对路由键（routing key）进行「全字匹配」
     */
    @Bean
    public DirectExchange chartExchange() {
        log.info("创建正常业务交换机：{}", CHART_EXCHANGE);
        return new DirectExchange(CHART_EXCHANGE, true, false);
    }

    /**
     * 正常业务队列：带死信配置，正常消费者监听
     * 当消息被拒绝（basicNack/reject）且 requeue=false 时，自动转死信
     */
    @Bean
    public Queue chartQueue() {
        log.info("创建正常业务队列：{}", CHART_QUEUE);
        return QueueBuilder.durable(CHART_QUEUE)
                // 死信配置：失败消息转到这里
                .withArgument("x-dead-letter-exchange", CHART_DLX_EXCHANGE)
                .withArgument("x-dead-letter-routing-key", CHART_DLX_ROUTING_KEY)
                // 消息TTL：10分钟未消费自动过期（可选，进死信或丢弃）
                .withArgument("x-message-ttl", 600000)
                .build();
    }

    /**
     * 正常业务绑定：交换机 + 队列 + 路由键
     */
    @Bean
    public Binding chartBinding() {
        log.info("创建正常业务绑定：交换机 {} 队列 {} 路由键 {}", CHART_EXCHANGE, CHART_QUEUE, CHART_ROUTING_KEY);
        return BindingBuilder.bind(chartQueue())
                .to(chartExchange())
                .with(CHART_ROUTING_KEY);
    }

    // ==================== 死信配置 ====================

    /**
     * 死信交换机：接收失败消息
     * 这里使用 Direct交换机对路由键（routing key）进行「全字匹配」
     */
    @Bean
    public DirectExchange chartDlxExchange() {
        log.info("创建死信交换机：{}", CHART_DLX_EXCHANGE);
        return new DirectExchange(CHART_DLX_EXCHANGE, true, false);
    }

    /**
     * 死信队列：监听失败消息，人工介入处理
     */
    @Bean
    public Queue chartDeadLetterQueue() {
        log.info("创建死信队列：{}", CHART_DLX_QUEUE);
        return QueueBuilder.durable(CHART_DLX_QUEUE).build();
    }

    /**
     * 死信绑定
     */
    @Bean
    public Binding dlxBinding() {
        log.info("创建死信绑定：交换机 {} 队列 {} 路由键 {}", CHART_DLX_EXCHANGE, CHART_DLX_QUEUE, CHART_DLX_ROUTING_KEY);
        return BindingBuilder.bind(chartDeadLetterQueue())
                .to(chartDlxExchange())
                .with(CHART_DLX_ROUTING_KEY);
    }

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}
