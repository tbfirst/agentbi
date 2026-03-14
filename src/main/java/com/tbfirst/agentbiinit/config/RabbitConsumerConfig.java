package com.tbfirst.agentbiinit.config;

import org.springframework.amqp.core.AcknowledgeMode;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.retry.backoff.ExponentialBackOffPolicy;
import org.springframework.retry.backoff.FixedBackOffPolicy;
import org.springframework.retry.policy.SimpleRetryPolicy;
import org.springframework.retry.support.RetryTemplate;

/**
 * rabbitmq 消费者线程池工厂配置
 */
@Configuration
public class RabbitConsumerConfig {

    @Bean("chartListenerContainerFactory")
    public SimpleRabbitListenerContainerFactory chartListenerContainerFactory(
            ConnectionFactory connectionFactory) {
        // 创建线程池工厂
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        // 设置连接工厂
        factory.setConnectionFactory(connectionFactory);

        // 并发消费者数（同时处理几个任务）
        factory.setConcurrentConsumers(16);
        // 最大并发
        factory.setMaxConcurrentConsumers(20);

        // 每次预取1条（公平分配，避免忙闲不均）
        factory.setPrefetchCount(1);

        // 消息转换器
        factory.setMessageConverter(new Jackson2JsonMessageConverter());

        // 设置手动 ACK（系统默认是自动 ACK）
        factory.setAcknowledgeMode(AcknowledgeMode.MANUAL);

        // 失败重试（本地重试3次，然后进死信）
        factory.setRetryTemplate(retryTemplate());
        factory.setDefaultRequeueRejected(false);

        return factory;
    }
    // 重试模板
//    @Bean
//    public RetryTemplate retryTemplate() {
//        RetryTemplate template = new RetryTemplate();
//
//        // 重试3次，间隔1秒
//        SimpleRetryPolicy policy = new SimpleRetryPolicy();
//        policy.setMaxAttempts(3);
//
//        FixedBackOffPolicy backOff = new FixedBackOffPolicy();
//        backOff.setBackOffPeriod(1000);
//
//        template.setRetryPolicy(policy);
//        template.setBackOffPolicy(backOff);
//
//        return template;
//    }
    // RabbitConsumerConfig.java
    @Bean
    public RetryTemplate retryTemplate() {
        RetryTemplate template = new RetryTemplate();

        SimpleRetryPolicy policy = new SimpleRetryPolicy();
        policy.setMaxAttempts(1); // 本地只重试1次，或完全不重试，让MQ重试

        // 指数退避：1s, 2s, 4s
        ExponentialBackOffPolicy backOff = new ExponentialBackOffPolicy();
        backOff.setInitialInterval(1000);
        backOff.setMultiplier(2);
        backOff.setMaxInterval(10000);

        template.setRetryPolicy(policy);
        template.setBackOffPolicy(backOff);
        return template;
    }

// 或者：关闭本地重试，让死信队列处理
// factory.setRetryTemplate(null);
// factory.setDefaultRequeueRejected(false); // 直接进死信，人工处理
}
