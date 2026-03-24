package com.tbfirst.agentbiinit.aichatmemorystore;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.tbfirst.agentbiinit.model.enums.AiRedisEnum;
import dev.langchain4j.data.message.*;
import dev.langchain4j.store.memory.chat.ChatMemoryStore;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;


/**
 * 基于 Redis 实现的会话记忆存储
 * 核心是重写 ChatMemoryStore 接口的方法，使用 Redis 存储会话信息
 */
@Repository
@Slf4j
public class RedisChatMemoryStore implements ChatMemoryStore {
    @Autowired
    private StringRedisTemplate redisTemplate;
    // 本地兜底：Redis不可用时，降级为内存存储（仅当前实例）
    private final Map<Object, List<ChatMessage>> localFallback = new ConcurrentHashMap<>();

    // 必须注入自定义的 ObjectMapper，不能 new ObjectMapper()，解决Jackson 不支持 Java8 日期时间问题
    @Autowired
    private ObjectMapper objectMapper;
    // 设置最大存储消息数量
    private static final int MAX_MESSAGES = 8; // 增加存储量以保留更多上下文
    // 消息长度阈值（字符数）
    private static final int MESSAGE_LENGTH_THRESHOLD = 1500;
    // ECharts配置的关键路径标识
    private static final String CHART_CONFIG_PATH = "chartConfig";
    private static final String ECHARTS_OPTION_PATH = "echartsOption";
    @Override
    public List<ChatMessage> getMessages(Object memoryId) {
        try{
            List<ChatMessage> result = new ArrayList<>();

            // 1. 读取 SystemMessage（长期）【原代码没有正确合并SystemMessage】
            String systemJson = redisTemplate.opsForValue()
                    .get(AiRedisEnum.SYSTEM_PROMPT.getValue() + "bi_template"); // 按用户隔离
            if (StringUtils.hasText(systemJson)) {
                result.addAll(ChatMessageDeserializer.messagesFromJson(systemJson));
            }

            // 2. 读取对话消息（短期）
            String json = redisTemplate.opsForValue().get(memoryId.toString());
            if (StringUtils.hasText(json)) {
                result.addAll(ChatMessageDeserializer.messagesFromJson(json));
            }

            return result;
        } catch (RedisConnectionFailureException e) {
            log.error("Redis不可用，降级本地存储(即沿用langchain4j的实现而非自己的实现), memoryId={}", memoryId);
            return localFallback.getOrDefault(memoryId, new ArrayList<>());
        }
    }

    // 重写 updateMessages 方法，实现压缩大消息和截断存储，完整保留 ECharts配置
    // 且实现当 redis 宕机实现本地内存进行暂时降级缓存
    @Override
    public void updateMessages(Object memoryId, List<ChatMessage> list) {
        try{
            if (list == null || list.isEmpty()) {
                return;
            }

            // 1. 分离 SystemMessage 并单独存储（长期缓存）
            List<ChatMessage> systemMessages = list.stream()
                    .filter(msg -> msg instanceof SystemMessage)
                    .collect(Collectors.toList());

            if (!systemMessages.isEmpty()) {
                String systemKey = AiRedisEnum.SYSTEM_PROMPT.getValue() + "bi_template";
                String systemJson = ChatMessageSerializer.messagesToJson(systemMessages);
                redisTemplate.opsForValue().set(systemKey, systemJson, Duration.ofDays(30));
            }

            // 2. 处理非SystemMessage（用户消息和AI回复）
            // 简单来说就是将非SystemMessage 筛选出来
            List<ChatMessage> nonSystemMessages = list.stream()
                    .filter(msg -> !(msg instanceof SystemMessage))
                    .collect(Collectors.toList());

            // 3. 压缩非SystemMessage 中的 AiMessage大消息，但完整保留ECharts配置
            List<ChatMessage> processedMessages = nonSystemMessages.stream()
                    .map(this::compressMessagePreservingECharts)
                    .collect(Collectors.toList());

            // 4. 智能截断：保留最近的消息，同时确保上下文连贯性
            List<ChatMessage> trimmedMessages = intelligentTruncate(processedMessages);

            // 5. 序列化并存储到Redis（TTL 2小时）
            String json = ChatMessageSerializer.messagesToJson(trimmedMessages);
            redisTemplate.opsForValue().set(
                    memoryId.toString(),
                    json,
                    Duration.ofHours(2)
            );
        } catch (RedisConnectionFailureException e) {
            log.error("Redis不可用，降级本地存储(即沿用langchain4j的实现而非自己的实现), memoryId={}", memoryId);
            // 降级本地存储（仅当前实例有效，重启丢失）
            localFallback.put(memoryId, list);
        }
    }

    /**
     * 压缩消息但完整保留ECharts配置
     */
    private ChatMessage compressMessagePreservingECharts(ChatMessage msg) {
        // 只处理AI消息，用户消息保持原样
        if (!(msg instanceof AiMessage)) {
            return msg;
        }

        AiMessage aiMsg = (AiMessage) msg;
        String content = aiMsg.text();

        // 如果消息长度未超过阈值，直接返回
        if (content.length() <= MESSAGE_LENGTH_THRESHOLD) {
            return msg;
        }

        try {
            // 解析JSON内容
            JsonNode rootNode = objectMapper.readTree(content);

            // 创建压缩后的结果对象
            ObjectNode compressedNode = objectMapper.createObjectNode();

            // 1. 保留analysis字段（分析结论）
            if (rootNode.has("analysis")) {
                compressedNode.set("analysis", rootNode.get("analysis"));
            }

            // 2. 完整保留chartConfig中的ECharts代码
            if (rootNode.has(CHART_CONFIG_PATH)) {
                // 重要：完整保留chartConfig，不做任何截断
                compressedNode.set(CHART_CONFIG_PATH, rootNode.get(CHART_CONFIG_PATH));
            }

            // 3. 保留echartsOption（如果存在且与chartConfig不同）
            if (rootNode.has(ECHARTS_OPTION_PATH)) {
                compressedNode.set(ECHARTS_OPTION_PATH, rootNode.get(ECHARTS_OPTION_PATH));
            }

            // 4. 保留关键的元数据字段
            preserveMetadataFields(rootNode, compressedNode);

            // 5. 添加压缩标记
            compressedNode.put("compressed", true);
            compressedNode.put("originalLength", content.length());
            compressedNode.put("compressedLength", compressedNode.toString().length());

            return new AiMessage(compressedNode.toString());

        } catch (Exception e) {
            // 解析失败时，返回包含基本信息的压缩消息
            return createFallbackCompressedMessage(content);
        }
    }

    /**
     * 保留关键的元数据字段
     */
    private void preserveMetadataFields(JsonNode source, ObjectNode target) {
        // 保留文件相关信息
        String[] metadataFields = {
                "fileName", "fileType", "timestamp", "dataSummary",
                "dataDimensions", "rowCount", "columnCount"
        };

        for (String field : metadataFields) {
            if (source.has(field)) {
                target.set(field, source.get(field));
            }
        }
    }

    /**
     * 创建降级压缩消息
     */
    private AiMessage createFallbackCompressedMessage(String originalContent) {
        ObjectNode fallbackNode = objectMapper.createObjectNode();
        fallbackNode.put("compressed", true);
        fallbackNode.put("note", "消息已压缩，ECharts配置完整保留");

        // 尝试从原始内容中提取ECharts配置（正则匹配）
        String echartsPattern = "\"chartConfig\"\\s*:\\s*\\{[^}]+}";
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile(echartsPattern);
        java.util.regex.Matcher matcher = pattern.matcher(originalContent);

        if (matcher.find()) {
            String chartConfigSection = matcher.group();
            try {
                // 尝试解析提取的部分
                JsonNode chartConfig = objectMapper.readTree("{" + chartConfigSection + "}");
                fallbackNode.set("chartConfig", chartConfig.get("chartConfig"));
            } catch (Exception e) {
                fallbackNode.put("chartConfigExtracted", false);
            }
        }

        return new AiMessage(fallbackNode.toString());
    }

    /**
     * 智能截断策略：保留上下文连贯性
     */
    private List<ChatMessage> intelligentTruncate(List<ChatMessage> messages) {
        if (messages.size() <= MAX_MESSAGES) {
            return messages;
        }

        List<ChatMessage> temp = new ArrayList<>(); // 逆序收集消息
        int remaining = MAX_MESSAGES;
        int i = messages.size() - 1;

        while (i >= 0 && remaining > 0) {
            ChatMessage current = messages.get(i);

            // 检测是否为完整的用户-AI消息对（AI消息在前一条是用户消息的情况下）
            if (current instanceof AiMessage && i > 0 && messages.get(i - 1) instanceof UserMessage) {
                if (remaining >= 2) {
                    // 保留完整的一对，注意添加顺序为逆序：先AI后用户，反转后即为正序（用户在前，AI在后）
                    temp.add(current);               // AI
                    temp.add(messages.get(i - 1));   // User
                    remaining -= 2;
                    i -= 2;
                } else if (remaining == 1) {
                    // 空间不足，只保留AI消息（放弃对应的用户消息）
                    temp.add(current);
                    remaining--;
                    i--;
                } else {
                    break; // remaining == 0，无需继续
                }
            } else {
                // 单独的消息（用户消息、其他类型消息，或孤立的AI消息）
                temp.add(current);
                remaining--;
                i--;
            }
        }

        // 反转得到时间正序的列表
        Collections.reverse(temp);
        return temp;
    }

    @Override
    public void deleteMessages(Object memoryId) {
        try{
            // 删除会话内容
            redisTemplate.delete(memoryId.toString());
        } catch (RedisConnectionFailureException e) {
            log.error("Redis不可用，降级本地存储(即沿用langchain4j的实现而非自己的实现), memoryId={}", memoryId);
            // 降级本地存储（仅当前实例有效，重启丢失）
            localFallback.remove(memoryId);
        }
    }
}