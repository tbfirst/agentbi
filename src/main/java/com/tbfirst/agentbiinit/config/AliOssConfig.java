package com.tbfirst.agentbiinit.config;

import com.tbfirst.agentbiinit.model.entity.AliOssProperties;
import com.tbfirst.agentbiinit.utils.AliOssUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 配置类，用于创建和配置 AliOssUtils 工具类的 Bean
 * 由于 AliOssUtils 工具类只提供方法，并未注册到容器中，因此需要通过该配置类创建一个 Bean 并注册到容器中
 */
@Configuration
@Slf4j
public class AliOssConfig {

    /**
     * 创建 AliOssUtils 工具类的 Bean
     * @return
     */
    @Bean
    @ConditionalOnMissingBean   // 该注解是为了确保只有一个该工具类的 Bean 被创建
    // 通过 AliOssProperties（与配置文件相映射）赋值返回一个 AliOssUtil 的工具类的 Bean
    public AliOssUtils aliOssUtil(AliOssProperties aliOssProperties) {
        log.info("开始创建 AliOssUtil 工具类的 Bean，参数为：{}", aliOssProperties);

        return new AliOssUtils(aliOssProperties.getEndpoint()
                ,aliOssProperties.getAccessKeyId()
                ,aliOssProperties.getAccessKeySecret()
                ,aliOssProperties.getBucketName());
    }
}
