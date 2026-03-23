package com.tbfirst.agentbiinit.ragflow;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tbfirst.agentbiinit.model.entity.RagflowProperties;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * RAGFlow 知识库检索服务
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class RagflowKnowledgeService {

    private final RagflowProperties properties;
    private final ObjectMapper objectMapper;

    private OkHttpClient httpClient;

    private OkHttpClient getHttpClient() {
        if (httpClient == null) {
            httpClient = new OkHttpClient.Builder()
                    .connectTimeout(30, TimeUnit.SECONDS)
                    .readTimeout(60, TimeUnit.SECONDS)
                    .build();
        }
        return httpClient;
    }

    /**
     * 检索 ECharts 相关知识
     */
    public List<RetrievalResult> retrieveEchartsKnowledge(String query) {
        String url = properties.getApiUrl() + "/api/retrieval";

        try {
            RetrievalRequest request = RetrievalRequest.builder()
                    .kbId(properties.getEchartsName())
                    .query(query)
                    .similarityThreshold(properties.getSimilarityThreshold())
                    .build();

            String jsonBody = objectMapper.writeValueAsString(request);
            RequestBody body = RequestBody.create(
                    jsonBody,
                    MediaType.parse("application/json; charset=utf-8")
            );

            Request httpRequest = new Request.Builder()
                    .url(url)
                    .header("Authorization", "Bearer " + properties.getApiKey())
                    .header("Content-Type", "application/json")
                    .post(body)
                    .build();

            try (Response response = getHttpClient().newCall(httpRequest).execute()) {
                if (!response.isSuccessful()) {
                    log.error("RAGFlow 检索失败: {}, body: {}",
                            response.code(), response.body().string());
                    return Collections.emptyList();
                }

                String responseBody = response.body().string();
                RetrievalResponse retrievalResponse = objectMapper.readValue(
                        responseBody,
                        RetrievalResponse.class
                );

                log.info("RAGFlow 检索成功，命中 {} 条知识",
                        retrievalResponse.getData().size());

                return retrievalResponse.getData();
            }
        } catch (Exception e) {
            log.error("RAGFlow 检索异常", e);
            return Collections.emptyList();
        }
    }

    // DTO 定义
    @Data
    @Builder
    public static class RetrievalRequest {
        private String kbId;
        private String query;
        private int topK;
        private double similarityThreshold;
    }

    @Data
    public static class RetrievalResponse {
        private int code;
        private String message;
        private List<RetrievalResult> data;
    }

    @Data
    public static class RetrievalResult {
        private String id;
        private String content;
        private String title;
        private double similarity;
        private Map<String, Object> metadata;
    }
}