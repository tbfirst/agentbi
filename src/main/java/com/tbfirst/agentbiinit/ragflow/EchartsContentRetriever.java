package com.tbfirst.agentbiinit.ragflow;

import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.rag.content.Content;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.data.document.Metadata;      // 注意引入的是 TextSegment 中的 Metadata
import dev.langchain4j.rag.query.Query;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * 自定义 ECharts 内容检索器
 * 使用 ElasticSearch 嵌入存储进行检索
 * 该类本质上是一个内容检索器，用于从 RAGFlow 知识库中检索相关内容（仅检索，不做增强）
 *
 * 【职责】：从 RAGFlow 知识库中检索相关内容（仅检索，不做增强）
 * 【对应 RAG 环节】：检索阶段（Retrieve）
 */
//@Component
@Slf4j
@RequiredArgsConstructor
public class EchartsContentRetriever implements ContentRetriever {

    private final ObjectMapper objectMapper;

    @Value("${ragflow.api-url:http://localhost:9380}")
    private String apiUrl;

    @Value("${ragflow.api-key:}")
    private String apiKey;

    @Value("${ragflow.echarts-kb-id:}")
    private String echartsKbId;

    @Value("${ragflow.top-k:5}")
    private int topK;

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(30))
            .build();

    @Override
    public List<Content> retrieve(Query query) {
        String searchQuery = extractKeywords(query.text());
        log.info("检索 ECharts 知识库，查询: {}", searchQuery);

        try {
            // 调用 RAGFlow 检索 API
            Map<String, Object> requestBody = Map.of(
                    "kb_id", echartsKbId,
                    "query", searchQuery,
                    "top_k", topK
            );

            String jsonBody = objectMapper.writeValueAsString(requestBody);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(apiUrl + "/api/retrieval"))
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                    .build();

            HttpResponse<String> response = httpClient.send(
                    request,
                    HttpResponse.BodyHandlers.ofString()
            );

            if (response.statusCode() != 200) {
                log.error("RAGFlow 检索失败: {}", response.body());
                return List.of();
            }

            RagflowResponse result = objectMapper.readValue(
                    response.body(),
                    RagflowResponse.class
            );

            // 转换为 LangChain4j Content 对象
            return result.getData().stream()
                    .map(this::toContent)
                    .collect(Collectors.toList());

        } catch (Exception e) {
            log.error("RAGFlow 检索异常", e);
            return List.of(); // 降级：返回空列表
        }
    }

    /**
     * 提取检索关键词（优化检索效果）
     */
    private String extractKeywords(String query) {
        StringBuilder keywords = new StringBuilder();

        // 提取图表类型
        if (query.contains("饼图") || query.contains("pie")) {
            keywords.append("echarts pie chart ");
        } else if (query.contains("折线图") || query.contains("line")) {
            keywords.append("echarts line chart ");
        } else if (query.contains("柱状图") || query.contains("bar")) {
            keywords.append("echarts bar chart ");
        }

        // 提取配置项
        if (query.contains("tooltip")) keywords.append("tooltip ");
        if (query.contains("legend")) keywords.append("legend ");
        if (query.contains("axis")) keywords.append("xAxis yAxis ");

        return keywords.length() > 0 ? keywords.toString() : query;
    }

    private Content toContent(RagflowResult result) {
        // 1. 构建内容文本
        String text = String.format("## %s\n%s", result.getTitle(), result.getContent());

        // 2. 创建元数据
        Map<String, String> map = Map.of(
                "title", result.getTitle(),
                "similarity", String.valueOf(result.getSimilarity()),
                "source_id", result.getId()
        );
        Metadata metadata = new Metadata(map);      // 注意引入的是 TextSegment 中的 Metadata

        TextSegment textSegment = new TextSegment(text, metadata);
        // 3. 使用 Content.from() 工厂方法
        return Content.from(textSegment);
    }

    // DTO
    @lombok.Data
    public static class RagflowResponse {
        private int code;
        private List<RagflowResult> data;
    }

    @lombok.Data
    public static class RagflowResult {
        private String id;
        private String title;
        private String content;
        private double similarity;
    }
}