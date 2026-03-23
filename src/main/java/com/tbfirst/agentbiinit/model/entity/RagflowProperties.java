package com.tbfirst.agentbiinit.model.entity;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * RAGFlow 配置属性
 */
@Component
// 从 application.yml 中读取 ragflow 前缀的配置,并绑定到 RagflowProperties 类的属性上
@ConfigurationProperties(prefix = "ragflow")
@Data
public class RagflowProperties {

    /** RAGFlow API 地址 */
    private String apiUrl = "http://localhost:9380";

    /** API Key */
    private String apiKey;              // 从 application.yml 中读取 ragflow.apiKey 配置

    /** ECharts 知识库 ID */
    private String echartsName;         // 从 application.yml 中读取 ragflow.echartsName 配置

    /** 相似度阈值 */
    private double similarityThreshold = 0.7;
}