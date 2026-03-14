package com.tbfirst.agentbiinit.model.entity;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 配置属性类，用于绑定配置文件中 aliyun.oss 相关的配置属性
 */
@Component
// @ConfigurationProperties 注解可将配置文件中的 aliyun.oss 变量属性映射到该类，与之对应
// 我在配置文件中使用使用 ${}从环境变量中获取，故这里就不需要使用 @Value注解
@ConfigurationProperties(prefix = "aliyun.oss")
@Data
public class AliOssProperties {
    private String endpoint;

    private String accessKeyId;

    private String accessKeySecret;

    private String bucketName;

}
