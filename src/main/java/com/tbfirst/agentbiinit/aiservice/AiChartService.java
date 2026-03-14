package com.tbfirst.agentbiinit.aiservice;

import com.tbfirst.agentbiinit.model.vo.AiAnalysisVO;
import dev.langchain4j.service.MemoryId;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;
import dev.langchain4j.service.spring.AiService;

import static dev.langchain4j.service.spring.AiServiceWiringMode.EXPLICIT;

/**
 * @author tbfirst
 * LangChain4j AI服务接口 - RAG增强版
 *
 * 【关键配置】
 * - wiringMode = EXPLICIT: 显式注入模式
 * - chatModel = "qwenPlusModel": 使用阿里云百炼 Qwen 模型
 * - chatMemoryProvider = "MyRedisChatMemoryProvider": Redis 持久化会话记忆
 * - retrievalAugmentor = "echartsRetrievalAugmentor": 【新增】RAGFlow 知识库检索增强
 */
@AiService(
        wiringMode = EXPLICIT,
        chatModel = "qwenPlusModel",
        chatMemoryProvider = "MyRedisChatMemoryProvider"
//        retrievalAugmentor = "echartsRetrievalAugmentor"  // 【关键】启用 RAG 增强
)
public interface AiChartService {

    /**
     * RAG 增强的图表分析生成
     *
     * 执行流程：
     * 1. retrievalAugmentor 拦截请求，从 RAGFlow 检索 ECharts 知识
     * 2. 将检索到的知识注入到 userPrompt 中
     * 3. 调用 qwenPlusModel 生成响应
     * 4. 返回结构化结果
     *
     * @param systemPrompt 系统提示词（控制模型行为）
     * @param userPrompt 用户提示词（会被 retrievalAugmentor 改写，注入知识库内容）
     * @param memoryId 会话记忆 ID（Redis 持久化）
     * @return 包含 ECharts 配置和分析结论的结构化结果
     */
    @SystemMessage("{{systemPrompt}}")
    @UserMessage("{{userPrompt}}")
    AiAnalysisVO analyzeAndGenerateChart(
            @V("systemPrompt") String systemPrompt,
            @V("userPrompt") String userPrompt,
            @MemoryId Object memoryId);
}