package com.tbfirst.agentbiinit.consumer;

import com.alibaba.dashscope.common.TaskStatus;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.Channel;
import com.tbfirst.agentbiinit.aiservice.AiChartService;
import com.tbfirst.agentbiinit.config.RabbitConfig;
import com.tbfirst.agentbiinit.model.entity.ChartTaskMessage2;
import com.tbfirst.agentbiinit.model.entity.TaskInfo;
import com.tbfirst.agentbiinit.model.enums.AiRedisEnum;
import com.tbfirst.agentbiinit.model.vo.AiAnalysisVO;
import com.tbfirst.agentbiinit.prompt.BiPromptTemplate;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

/**
 * @author tbfirst
 * rabbitmq 消费者
 * 实际上就是将原来的异步线程池中的业务代码转到消息队列的消费者完成
 */
@Component
@Slf4j
public class ChartTaskConsumer {
    @Autowired
    private StringRedisTemplate redisTemplate;
    @Autowired
    private AiChartService aiChartService;
    @Autowired
    private ObjectMapper objectMapper;

    /**
     * 正常业务时的消费者
     * @param message
     * @param channel
     * @param tag
     */

    // 版本五：传入的参数是 ChartTaskMessage2，基于 csvUrl 构建用户提示词
    @RabbitListener(queues = RabbitConfig.CHART_QUEUE,
            containerFactory = "chartListenerContainerFactory")
    public void handleChartTask(ChartTaskMessage2 message, Channel channel,
                                @Header(AmqpHeaders.DELIVERY_TAG) long tag) {

        String taskId = message.getTaskId();
        String taskKey = AiRedisEnum.CHART_TASK.getValue() + taskId;
        String consumerName = Thread.currentThread().getName();
        log.info("消费者【{}】开始正常业务的处理，任务ID：{}", consumerName, taskId);
        try {
            // 1. 去缓存中根据任务 id 更新状态为执行中
            updateTaskStatus(taskKey, TaskStatus.RUNNING, null);

            // 2. 原调用 AI 逻辑（在后台线程执行）
            String name = message.getName();
            String goal = message.getGoal();
            String chartType = message.getChartType();
            String csvUrl = message.getCsvUrl();
            String memoryId = message.getMemoryId();
            String fingerprint = message.getFingerprint();

            String userPrompt = BiPromptTemplate.buildUserPromptByCsvUrl(
                    csvUrl, goal, chartType, name);
            String systemPrompt = BiPromptTemplate.getSystemPrompt();

            // 【关键】消费者在调用 AI 前，先检查缓存是否有结果
            String cacheKey = AiRedisEnum.CHART_RESULT.getValue() + fingerprint;
            String cacheValue = redisTemplate.opsForValue().get(cacheKey);
            if (cacheValue != null) {
                log.info("【消费者缓存命中】taskId={}, 跳过AI", message.getTaskId());
                updateTaskStatus(taskKey, TaskStatus.SUCCEEDED, cacheValue);
                channel.basicAck(tag, false);
                return;
            }

            log.info("消费者【{}】开始调用 ai 服务，任务ID：{}", consumerName, taskId);
            // 【核心】调用 AI Service，自动触发 RAG 检索增强
            // RetrievalAugmentor 会自动：
            // 1. 从 userPrompt 中提取图表类型等关键词
            // 2. 检索 RAGFlow ECharts 知识库
            // 3. 将知识注入到 UserMessage 中
            // 4. 调用阿里云百炼大模型
            AiAnalysisVO result = aiChartService.analyzeAndGenerateChart(systemPrompt, userPrompt, memoryId);

            // 3. 存储结果和历史到缓存
            String resultJson = objectMapper.writeValueAsString(result);
            redisTemplate.opsForValue().set(cacheKey, resultJson, 1, TimeUnit.HOURS);
            String historyKey = AiRedisEnum.CHART_HISTORY.getValue() + memoryId + ":" + fingerprint;
            redisTemplate.opsForValue().set(historyKey, resultJson, 7, TimeUnit.DAYS);

            // 4. 去缓存中根据任务 id 更新状态为成功
            updateTaskStatus(taskKey, TaskStatus.SUCCEEDED, resultJson);

            // 5. 手动 ACK（确认消费成功）
            channel.basicAck(tag, false);

        } catch (Exception e) {
            // 6. 失败处理：拒绝消息，更新状态为失败 + 进入死信队列
            log.error("消费者【{}】ai 调用任务失败，任务ID：{}", consumerName, taskId, e);
            try {
                // 6.1. 更新失败状态
                updateTaskStatus(taskKey, TaskStatus.FAILED, e.getMessage());

                // 6.2 进死信队列，可配置重试
                // 参数说明：
                // tag：消息的唯一标签，用于确认或拒绝消息
                // requeue：是否重新入队，false 表示拒绝后不重新入队，直接进入死信队列
                // 可配置重试策略，如设置为 true 则会重新入队，等待后续重试
                log.error("消费者【{}】拒绝消息，准备进入死信队列，任务ID：{}", consumerName, taskId, e);
                channel.basicNack(tag, false, false);
            } catch (IOException ex) {
                log.error("拒绝消息失败", ex);
            }
        }
    }

    /**
     * 死信消费者：监听死信队列
     * @param message
     */
    // 添加 RabbitListener 注解，指定死信队列名称为 chart.dlx.queue（绑定到 chart.dlx.exchange）
    @RabbitListener(queues = RabbitConfig.CHART_DLX_QUEUE,
                    containerFactory = "chartListenerContainerFactory")
    public void handleDeadLetter(ChartTaskMessage2 message,Channel channel,
                                 @Header(AmqpHeaders.DELIVERY_TAG) long tag) {
        String consumerName = Thread.currentThread().getName();
        log.error("消费者【{}】任务进入死信队列，taskId={}", consumerName, message.getTaskId());

        // 1. 更新状态为失败
        updateTaskStatus(message.getTaskId(),TaskStatus.FAILED, "多次重试失败");

        // 2. todo 发送告警（钉钉/邮件）
//        sendAlert("AI任务失败: " + message.getTaskId());

        // 3. todo 落库持久化，人工介入
//        saveToFailLog(message);

        // 4. 手动 ACK（确认消费成功）
        try {
            channel.basicAck(tag, false);
        } catch (IOException e) {
            log.error("死信处理失败", e);
            // 死信处理失败，基本只能记录日志了
        }
    }

    /**
     * 修改任务状态
     * @param taskKey
     * @param status
     * @param data
     */
    private void updateTaskStatus(String taskKey, TaskStatus status, String data) {
        try {
            // 1、检查任务是否存在
            String json = redisTemplate.opsForValue().get(taskKey);
            if (json == null) return;   // 任务不存在，直接返回

            // 2、更新任务状态
            TaskInfo task = objectMapper.readValue(json, TaskInfo.class);
            task.setStatus(status);
            task.setUpdateTime(LocalDateTime.now());

            if (status == TaskStatus.SUCCEEDED) {
                task.setResult(data);
            } else if (status == TaskStatus.FAILED) {
                task.setErrorMsg(data);
            }
            // 3、将更新后的任务状态写回 Redis
            redisTemplate.opsForValue().set(taskKey,
                    objectMapper.writeValueAsString(task), 1, TimeUnit.HOURS);

        } catch (Exception e) {
            log.error("更新状态失败", e);
        }
    }
}