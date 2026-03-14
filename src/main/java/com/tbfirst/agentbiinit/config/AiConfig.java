package com.tbfirst.agentbiinit.config;

import com.tbfirst.agentbiinit.aichatmemorystore.RedisChatMemoryStore;
import com.tbfirst.agentbiinit.ragflow.EchartsContentRetriever;
import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.memory.chat.ChatMemoryProvider;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.input.PromptTemplate;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.rag.DefaultRetrievalAugmentor;
import dev.langchain4j.rag.RetrievalAugmentor;
import dev.langchain4j.rag.content.injector.ContentInjector;
import dev.langchain4j.rag.content.injector.DefaultContentInjector;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

/**
 * ai 组件配置
 */
@Configuration
public class AiConfig {
    @Autowired
    private RedisChatMemoryStore redisChatMemoryStore;
    // @Autowired不可以和 final 关键字同时使用，使用@RequiredArgsConstructor可以解决这个问题
    // RequiredArgsConstructor 是 Lombok 库提供的一个注解
    // 用于在编译时自动生成一个包含所有 final 字段或标记为 @NonNull 的字段的构造方法
//    @Autowired
//    private EchartsContentRetriever echartsContentRetriever;

    /**
     * 自定义使用阿里云百炼的 qwen-plus 模型的 ChatLanguageModel
     * @return
     */
    @Bean("qwenPlusModel")  // 自定义 Bean 名称，与 @AiService 对应
    public ChatLanguageModel qwenPlusModel() {
        return OpenAiChatModel.builder()
                .baseUrl("https://dashscope.aliyuncs.com/compatible-mode/v1")
                .apiKey(System.getenv("DASHSCOPE_API_KEY"))  // 从环境变量读取
                .modelName("qwen-plus")  // 百炼模型名称
                .timeout(Duration.ofSeconds(60)) // 从默认10秒增加到60秒
                .maxRetries(2) // 网络层重试，不是业务重试
                .build();
    }
    /**
     * 公共的会话记忆配置
     */
    @Bean
    public ChatMemory MyChatMemory() {
        // LangChain4j 提供了两个可直接使用的实现： MessageWindowChatMemory、TokenWindowChatMemory
        // 若要根据消息数量限制记忆长度，可使用 MessageWindowChatMemory
        // 若要根据 Token 数量限制记忆长度，可使用 TokenWindowChatMemory
        return MessageWindowChatMemory.builder()
                .maxMessages(10)    // 会话记忆最多存储10条消息
                .build();
    }

    /**
     * 会话记忆隔离配置，即使用默认的 ChatMemoryProvider 即可
     * 每个用户会话都有一个独立的 ChatMemory，互不干扰
     */
    @Bean
    public ChatMemoryProvider MyChatMemoryProvider() {
        // 核心是重写 ChatMemoryProvider 中的 get 方法
        return new ChatMemoryProvider() {
            @Override
            public ChatMemory get(Object memoryId) {
                return MessageWindowChatMemory.builder()
                        .id(memoryId)       // 每个会话都有一个独立的 memoryId
                        .maxMessages(10)    // 会话记忆最多存储10条消息
                        .build();
            }
        };
    }

    /**
     * 会话记忆持久化，即使用自定义的 ChatMemoryProvider 即可
     * 默认会话记忆是在内存中，重启后丢失
     * 如果需要持久化，可以实现自定义的 ChatMemoryStore， 将 ChatMessage存储在您选择的任何持久化存储中
     */
    @Bean
    public ChatMemoryProvider MyRedisChatMemoryProvider() {
        // 核心是重写 ChatMemoryProvider 中的 get 方法
        return new ChatMemoryProvider() {
            @Override
            public ChatMemory get(Object memoryId) {
                return MessageWindowChatMemory.builder()
                        .id(memoryId)       // 每个会话都有一个独立的 memoryId
                        .maxMessages(5)    // 会话记忆最多存储5条消息
                        // 会话记忆存储在 Redis 中，使用自定义的 RedisChatMemoryStore
                        .chatMemoryStore(redisChatMemoryStore)
                        .build();
            }
        };
    }


    /**
     * 默认检索增强器（Easy RAG 提供）
     *
     * 【职责】： orchestrate 检索流程
     * 1. 调用 ContentRetriever 检索内容
     * 2. 使用 ContentInjector 将内容注入 Prompt
     * 3. 返回增强后的消息
     */
//    @Bean("echartsRetrievalAugmentor")
//    public RetrievalAugmentor retrievalAugmentor() {
//        return DefaultRetrievalAugmentor.builder()
//                .contentRetriever(echartsContentRetriever)
//                .contentInjector(contentInjector())
//                .build();
//    }
//
//    /**
//     * 内容注入器（定义如何将检索内容插入 Prompt）
//     */
//    @Bean
//    public ContentInjector contentInjector() {
//        return DefaultContentInjector.builder()
//            // 自定义提示词模板
//            .promptTemplate(new PromptTemplate("""
//                【ECharts 知识库参考】
//                以下是从官方文档检索到的相关资料，生成代码时必须严格参考：
//
//                {{contents}}
//
//                【重要约束】
//                1. 必须严格使用上述知识库中的 ECharts 配置项
//                2. 禁止编造知识库中未提及的配置项
//                3. 确保生成的 option 符合 ECharts 5.x 规范
//
//                【用户问题】
//                {{userMessage}}
//                """)
//            )
//        .build();
//    }

}
