package com.tbfirst.agentbiinit.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.tbfirst.agentbiinit.common.ErrorCode;
import com.tbfirst.agentbiinit.config.RabbitConfig;
import com.tbfirst.agentbiinit.exception.BusinessException;
import com.tbfirst.agentbiinit.manager.RedissonLimitManager;
import com.tbfirst.agentbiinit.mapper.ChartMapper;
import com.tbfirst.agentbiinit.mapper.FileTaskMapper;
import com.tbfirst.agentbiinit.model.dto.chart.ChartResultResponse;
import com.tbfirst.agentbiinit.model.dto.chart.GenerateChartByAiRequest;
import com.tbfirst.agentbiinit.model.entity.*;
import com.tbfirst.agentbiinit.model.enums.AiRedisEnum;
import com.tbfirst.agentbiinit.model.vo.AiAnalysisVO;
import com.tbfirst.agentbiinit.service.ChartService;
import com.tbfirst.agentbiinit.utils.*;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.binary.Hex;
import org.redisson.api.RBloomFilter;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import com.tbfirst.agentbiinit.service.UserService;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import com.tbfirst.agentbiinit.model.dto.chart.FileTaskStatusResponse;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;


/**
* @description 针对表【chart(智能 bi 图表信息表)】的数据库操作Service实现
*/
@Service
@Slf4j
public class ChartServiceImpl extends ServiceImpl<ChartMapper, ChartEntity> implements ChartService {
    @Autowired
    private StringRedisTemplate redisTemplate;
    @Autowired
    private RedissonClient redissonClient;
    @Autowired
    private RedissonLimitManager redissonLimitManager;
    @Autowired
    private RabbitTemplate rabbitTemplate;
    // 必须注入自定义的 ObjectMapper，不能 new ObjectMapper()，解决Jackson 不支持 Java8 日期时间问题
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private FileTaskMapper fileTaskMapper;
    @Autowired
    private AiFileSecurityScanner aiFileSecurityScanner;
    @Autowired
    private AliOssUtils aliOssUtils;
    // 注入自身代理
    @Autowired
    @Lazy
    private ChartServiceImpl self;
    // Redis布隆过滤器（Redisson提供）
    @Autowired
    private RBloomFilter<String> bloomFilter;

    @Autowired
    private UserService userService;

    // 使用异步线程池处理文件解析
    @Async("fileParserExecutor")        // ← 关键注解，丢给对应线程池执行
    public CompletableFuture<String> submitFileTaskAsync(String fileTaskId,
                                                         String ossUrl,
                                                         FileTaskInfo task,
                                                         String userId,
                                                         String fingerprint,
                                                         GenerateChartByAiRequest generateChartByAiRequest,
                                                         String memoryId
                                                         ) {
        String threadName = Thread.currentThread().getName();
        log.info("异步线程 {} 开始处理文件解析任务 {}", threadName, fileTaskId);

        long startTime = System.currentTimeMillis();

        // 1. 可选：Redis 缓存短期文件解析任务状态（加速查询，TTL 5分钟）
        String redisKey = AiRedisEnum.FILE_TASK.getValue() + fileTaskId;
        redisTemplate.opsForValue().set(redisKey, "INIT", 5, TimeUnit.MINUTES);

        try {
            // 更新 mysql 中的任务状态
            log.info("异步线程 {} 更新mysql中的文件解析任务 {} 状态为 RUNNING", threadName, fileTaskId);
            task.setStatus("RUNNING");
            task.setUpdatedTime(LocalDateTime.now());
            fileTaskMapper.updateById(task);
            // 更新 redis 中的任务状态
            redisTemplate.opsForValue().set(redisKey, "PROCESSING", 5, TimeUnit.MINUTES);

            // 2. 【关键】执行文件解析任务
            log.info("异步线程 {} 开始 OSS下载 → 解析 → OSS上传文件解析任务 {}", threadName, fileTaskId);
            String csvUrl = parseAndUploadToOss(ossUrl, fileTaskId);

            // 更新成功状态
            log.info("异步线程 {} 解析完成任务 {}，更新 CSV URL 为 {}", threadName, fileTaskId, csvUrl);
            long costTime = System.currentTimeMillis() - startTime;
            task.setStatus("SUCCEEDED");     // 更新任务状态为 SUCCEEDED
            task.setCsvUrl(csvUrl);
            task.setParseTimeMs((int) costTime);
            task.setUpdatedTime(LocalDateTime.now());
            fileTaskMapper.updateById(task);  // 更新实体

            // 3. 【关键】异步 ai 调用任务（只有当文件解析成功后生成了有效的 csvUrl 才触发）
            // 3.1 生成 rabbitmq 处理 ai 调用任务的任务ID，确保唯一
            String taskId = userId + "-" + UUID.randomUUID().toString();
            // 3.2 构建将发送给 rabbitmq 的消息
            log.info("异步线程 {} 开始准备信息发送给 rabbitmq 处理 ai 调用任务 {}", threadName, taskId);
            ChartTaskMessage2 message = ChartTaskMessage2.builder()
                    .taskId(taskId)
                    .fingerprint(fingerprint)
                    .userId(userId)
                    .memoryId(memoryId)
                    .csvUrl(csvUrl)  // 注意：传入的是解析后的 CSV 文件 URL
                    .goal(generateChartByAiRequest.getGoal())
                    .chartType(generateChartByAiRequest.getChartType())
                    .name(generateChartByAiRequest.getName())
                    .timestamp(System.currentTimeMillis())
                    .build();
            // 3.3 初始化 rabbitmq 处理 ai 调用任务的任务状态
            String taskKey = AiRedisEnum.CHART_TASK.getValue() + taskId;
            redisTemplate.opsForValue().set(taskKey,
                    objectMapper.writeValueAsString(message), 1, TimeUnit.HOURS);
            // 3.4 【关键】发送信息到 RabbitMQ
            // （注意已将原传入的 csvData 替换为 csvUrl,需要修改消费者中原 ai 调用服务的相关代码）
            log.info("发送消息到 RabbitMQ，任务ID：{}", taskId);
            rabbitTemplate.convertAndSend(
                    RabbitConfig.CHART_EXCHANGE,
                    RabbitConfig.CHART_ROUTING_KEY,
                    message,
                    msg -> {
                        // 消息持久化
                        msg.getMessageProperties().setDeliveryMode(MessageDeliveryMode.PERSISTENT);
                        // 消息ID用于幂等
                        msg.getMessageProperties().setMessageId(taskId);
                        return msg;
                    }
            );

            // Redis 标记成功（短 TTL，仅用于即时查询）
            redisTemplate.opsForValue().set(redisKey, "COMPLETED:" + csvUrl, 1, TimeUnit.MINUTES);

            return CompletableFuture.completedFuture(csvUrl);

        } catch (Exception e) {
            // 更新失败状态
            task.setStatus("FAILED");     // 更新任务状态为 FAILED
            task.setErrorMsg(e.getMessage());
            task.setUpdatedTime(LocalDateTime.now());
            fileTaskMapper.updateById(task);  // 更新实体

            redisTemplate.opsForValue().set(redisKey, "FAILED:" + e.getMessage(), 1, TimeUnit.MINUTES);

            return CompletableFuture.failedFuture(e);
        }
    }

    /**
     * 核心方法：OSS下载 → 解析 → OSS上传（带超时诊断）
     */
    private String parseAndUploadToOss(String ossUrl, String fileTaskId) throws IOException {
        String threadName = Thread.currentThread().getName();
        log.info("[{}] 开始 parseAndUploadToOss, fileTaskId={}", threadName, fileTaskId);

        // 1. 提取 objectName
        long step1Start = System.currentTimeMillis();
        String objectName = AliOssUtils.extractObjectNameFromUrl(ossUrl);
        String extension = FileParseUtils.getFileExtension(objectName);
        log.info("[{}] 步骤1-提取文件名完成: {}, 类型: {}, 耗时{}ms",
                threadName, objectName, extension, System.currentTimeMillis() - step1Start);

        // 2. 下载文件（带大小检查）
        long step2Start = System.currentTimeMillis();
        log.info("[{}] 步骤2-开始下载文件...", threadName);
        byte[] fileBytes = aliOssUtils.downloadFileBytes(objectName);
        log.info("[{}] 步骤2-下载完成, 大小: {} bytes, 耗时{}ms",
                threadName, fileBytes.length, System.currentTimeMillis() - step2Start);

        // 检查文件大小，超过 10MB 警告
        if (fileBytes.length > 10 * 1024 * 1024) {
            log.warn("[{}] 文件过大: {} MB，可能导致内存问题",
                    threadName, fileBytes.length / (1024 * 1024));
        }

        // 3. 解析为 CSV
        long step3Start = System.currentTimeMillis();
        log.info("[{}] 步骤3-开始解析文件...", threadName);
        byte[] csvBytes;
        try (ByteArrayInputStream inputStream = new ByteArrayInputStream(fileBytes)) {
            csvBytes = FileParseUtils.parseToCsvBytes(inputStream, extension);
        }
        log.info("[{}] 步骤3-解析完成, CSV大小: {} bytes, 耗时{}ms",
                threadName, csvBytes.length, System.currentTimeMillis() - step3Start);

        // 4. 上传 CSV 到 OSS
        long step4Start = System.currentTimeMillis();
        log.info("[{}] 步骤4-开始上传CSV...", threadName);
        String csvObjectName = "csv/" + fileTaskId + ".csv";
        String csvUrl = aliOssUtils.uploadFile(csvBytes, csvObjectName);
        log.info("[{}] 步骤4-上传完成, url={}, 耗时{}ms",
                threadName, csvUrl, System.currentTimeMillis() - step4Start);

        long totalTime = System.currentTimeMillis() - step1Start;
        log.info("[{}] parseAndUploadToOss 全部完成, 总耗时{}ms", threadName, totalTime);

        return csvUrl;
    }

    // 版本九：在八的基础上，修改多级缓存配置
    @Override
    public ChartResultResponse genChartByAiMQApache2(MultipartFile multipartFile,
                                        GenerateChartByAiRequest generateChartByAiRequest,
                                        HttpServletRequest httpRequest) {
        try {
            // 0. 生成指纹
            String fingerprint = generateFingerprint(multipartFile, generateChartByAiRequest);
            UserEntity loginUser = userService.getLoginUser();
            String userId = String.valueOf(loginUser.getId());
            String memoryId = (String) httpRequest.getAttribute("memoryId");
            if (memoryId == null) {
                memoryId = userId + "-chat";
                httpRequest.setAttribute("memoryId", memoryId);
            }
            // 1.1 检查一级缓存：只存历史缓存指针和时间戳
            String cacheKey = AiRedisEnum.CHART_RESULT.getValue() + fingerprint;
            String pointer = redisTemplate.opsForValue().get(cacheKey);
            if (pointer != null) {
                log.info("命中结果缓存，直接返回");
                String historyKey = pointer.split("\\|")[0];
                String resultJson = (String) redisTemplate.opsForHash().get(historyKey, "1");
                return buildChartResultFromJson(resultJson);
            }
            // 1.2 【布隆过滤器】快速判断是否可能存在
            // false = 肯定不存在，避免查数据库，直接去检查文件，上传文件以及之后的流程
            if (!bloomFilter.contains(fingerprint)) {
                log.info("布隆过滤器判定不存在，跳过二级缓存查询");
                return processNewFile(multipartFile, fingerprint, cacheKey, pointer, memoryId, userId, generateChartByAiRequest);
            }
            // 1.3 检查二级缓存：存用户历史缓存（7天TTL）
            String historyKey = AiRedisEnum.CHART_HISTORY.getValue() + memoryId + ":" + fingerprint;
            Map<Object, Object> historyMap = redisTemplate.opsForHash().entries(historyKey);
            boolean historyMapKey = historyMap.containsKey("1");
            // 如果二级缓存中存在历史数据（通过hashkey是否为1判断），预热后直接返回历史数据（无需重新AI调用）
            if (historyMapKey) {
                log.info("【L2命中】从历史恢复并预热L1, fingerprint={}", fingerprint);
                // 预热：复制到L1，重置1小时TTL（重建L1指针）
                // L1：存指针+时间戳（约100字节，1小时）
                String resultJson = (String) redisTemplate.opsForHash().get(historyKey, "1");
                pointer = historyKey + "|" + System.currentTimeMillis();
                redisTemplate.opsForValue().set(cacheKey, pointer, 1, TimeUnit.HOURS);
                return buildChartResultFromJson(resultJson);
            }
            // 1.4 布隆误判了（假阳性），实际不存在
            log.warn("布隆过滤器误判: {}", fingerprint);
            return processNewFile(multipartFile, fingerprint, cacheKey, pointer, memoryId, userId, generateChartByAiRequest);
        }
        catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "请求超时");
        }
    }
    private ChartResultResponse processNewFile(MultipartFile multipartFile,
                                  String fingerprint,
                                  String cacheKey,
                                  String pointer,
                                  String memoryId,
                                  String userId,
                                  GenerateChartByAiRequest generateChartByAiRequest) throws InterruptedException {
        String mainThread = Thread.currentThread().getName();
        // 2. Apache Tika 检查文件
        try {
            AiFileSecurityScanner.ScanResult scan = aiFileSecurityScanner.scan(multipartFile);
            log.info("文件扫描通过: {}, 类型: {}, 预估行数: {}",
                    multipartFile.getOriginalFilename(),
                    scan.getMimeType(),
                    scan.getRowCount());
        } catch (AiFileSecurityScanner.SecurityScanException e) {
            log.warn("文件扫描失败 [{}]: {}", e.getCode(), e.getMessage());
            throw new BusinessException(ErrorCode.FILE_SCAN_ERROR, e.getMessage());
        }

        // 3 上传用户文件到阿里云 OSS
        String ossUrl = null;
        try {
            ossUrl = aliOssUtils.uploadFile(multipartFile.getBytes(), multipartFile.getOriginalFilename());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        // 4. 使用 Redisson分布式锁，防止并发重复调用
        String lockKey = AiRedisEnum.CHART_LOCK.getValue() + fingerprint;
        RLock lock = redissonClient.getLock(lockKey);
        boolean locked = false;
        try {
            locked = lock.tryLock(1, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        if (!locked) {
            // 未获取到锁，等待并重试（轮询缓存）
            for (int i = 0; i < 10; i++) {
                Thread.sleep(300);
                pointer = redisTemplate.opsForValue().get(cacheKey);
                if (pointer != null) {
                    String historyKey = pointer.split("\\|")[0];
                    String resultJson = (String) redisTemplate.opsForHash().get(historyKey, "1");
                    if (resultJson != null) {
                        return buildChartResultFromJson(resultJson);
                    }
                }
            }
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "系统异常，请稍后重试");
        }

        try {
            // 5. 双重检查：可能在等待锁的过程中其他线程已写入缓存
            pointer = redisTemplate.opsForValue().get(cacheKey);
            if (pointer != null) {
                String historyKey = pointer.split("\\|")[0];
                String resultJson = (String) redisTemplate.opsForHash().get(historyKey, "1");
                if (resultJson != null) {
                    return buildChartResultFromJson(resultJson);
                }
            }

            // 6. 限流（在提交任务前执行，避免任务堆积）
            String limitKey = AiRedisEnum.CHART_LIMIT.getValue() + memoryId;
            int rate = 2; // 每秒生成2个令牌
            int bucketSize = 5; // 令牌桶容量为5
            redissonLimitManager.initRateLimiter(limitKey, rate, bucketSize);
            // 检查是否超过限流阈值
            if (redissonLimitManager.isOverLimit(limitKey)) {
                throw new BusinessException(ErrorCode.TOO_MANY_REQUESTS, "请求频率过快，请稍后重试");
            }

            // 7. 异步文件解析任务
            // 7.1 生成异步线程池处理文件解析任务的任务ID，确保唯一
            String fileTaskId = userId + "-file-" + UUID.randomUUID().toString();
            // 7.2 初始化异步线程池处理文件解析任务的任务
            log.info("初始化文件解析任务状态并存入 mysql，任务ID：{}", fileTaskId);
            // 构建实体对象，MyBatis-Plus 自动映射字段
            FileTaskInfo task = FileTaskInfo.builder()
                    .fileTaskId(fileTaskId)
                    .userId(userId)
                    .fingerprint(fingerprint)
                    .originalUrl(ossUrl)
                    .status("PENDING")  // 初始化刚开始的文件任务解析状态为 PENDING
                    .createdTime(LocalDateTime.now())
                    .build();
            fileTaskMapper.insert(task);  // 插入实体
            // 7.3 【关键】直接提交异步处理文件解析任务
            log.info("【主线程 {}】提交异步文件解析任务给异步线程池，fileTaskId={}", mainThread, fileTaskId);
            // 【关键】通过注入自身代理调用异步方法，确保在当前类中执行
            // Spring 代理机制：@Async 通过 AOP 代理实现，只有外部调用才会触发代理，同类自调用会绕过代理
            // 【关键】该文件解析内部会执行 rabbitmq 的异步调用 ai 服务
            self.submitFileTaskAsync(fileTaskId, ossUrl, task, userId, fingerprint, generateChartByAiRequest, memoryId);

            // 8. 返回任务已提交响应，AI在后台运行
            ChartResultResponse response = new ChartResultResponse();
            response.setStatus("PENDING");
            response.setFingerprint(fingerprint);
            response.setFileTaskId(fileTaskId);
            return response;
        } finally {
            lock.unlock();
        }
    }


    // 指纹版本三：进行空值处理以及改用 Java 标准库 SHA-256替代 md5
    private String generateFingerprint(MultipartFile file,
                                       GenerateChartByAiRequest request) {
        try {

            byte[] fileBytes = file.getBytes();
            // 1. 文件内容哈希
            String fileFirst256Hex = sha256Hex(fileBytes);

            // 2. 构建有序参数Map，确保参数顺序不影响指纹结果
            Map<String, String> params = new TreeMap<>();
            params.put("fileHash", fileFirst256Hex);
            params.put("goal", nullToEmpty(request.getGoal()));
            params.put("chartType", nullToEmpty(request.getChartType()));
            params.put("name", nullToEmpty(request.getName()));

            // 3. JSON序列化（确定性输出）
            String content = objectMapper.writeValueAsString(params);

            // 4. 返回最终哈希
            return sha256Hex(content.getBytes(StandardCharsets.UTF_8));

        } catch (IOException e) {
            throw new RuntimeException("生成指纹失败", e);
        }
    }

    private String nullToEmpty(String str) {
        return str == null ? "" : str;
    }
    private String sha256Hex(byte[] data) {
        try {
            return Hex.encodeHexString(MessageDigest.getInstance("SHA-256").digest(data));
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public FileTaskStatusResponse getFileTaskStatus(String fileTaskId) {
        QueryWrapper<FileTaskInfo> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("file_task_id", fileTaskId);
        FileTaskInfo fileTaskInfo = fileTaskMapper.selectOne(queryWrapper);

        if (fileTaskInfo == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "文件任务不存在");
        }

        FileTaskStatusResponse response = new FileTaskStatusResponse();
        response.setFileTaskId(fileTaskInfo.getFileTaskId());
        response.setStatus(fileTaskInfo.getStatus());
        response.setFingerprint(fileTaskInfo.getFingerprint());
        response.setCsvUrl(fileTaskInfo.getCsvUrl());
        response.setErrorMsg(fileTaskInfo.getErrorMsg());
        response.setParseTimeMs(fileTaskInfo.getParseTimeMs());
        response.setFileSize(fileTaskInfo.getFileSize());
        response.setCsvSize(fileTaskInfo.getCsvSize());

        return response;
    }

    @Override
    public ChartResultResponse getChartResultByFingerprint(String fingerprint) {
        ChartResultResponse response = new ChartResultResponse();
        response.setStatus("PENDING");

        String cacheKey = AiRedisEnum.CHART_RESULT.getValue() + fingerprint;
        String pointer = redisTemplate.opsForValue().get(cacheKey);

        if (pointer != null) {
            String historyKey = pointer.split("\\|")[0];
            String resultJson = (String) redisTemplate.opsForHash().get(historyKey, "1");

            if (resultJson != null) {
                try {
                    AiAnalysisVO analysisVO = objectMapper.readValue(resultJson, AiAnalysisVO.class);
                    response.setChartConfig(analysisVO.getChartConfig());
                    // 转换 ECharts 配置字段名大小写
                    ObjectNode chartConfig = (ObjectNode) objectMapper.valueToTree(analysisVO.getChartConfig());
                    if (chartConfig.has("xaxis")) {
                        chartConfig.set("xAxis", chartConfig.get("xaxis"));
                        chartConfig.remove("xaxis");
                    }
                    if (chartConfig.has("yaxis")) {
                        chartConfig.set("yAxis", chartConfig.get("yaxis"));
                        chartConfig.remove("yaxis");
                    }
                    String chartConfigJson = objectMapper.writeValueAsString(chartConfig);
                    response.setEchartsCode(chartConfigJson);
                    
                    StringBuilder analysisBuilder = new StringBuilder();
                    analysisBuilder.append("**整体概况：\n").append(analysisVO.getAnalysis().getSummary()).append("\n\n");
                    
                    if (analysisVO.getAnalysis().getKeyFindings() != null && !analysisVO.getAnalysis().getKeyFindings().isEmpty()) {
                        analysisBuilder.append("**关键发现：\n");
                        for (int i = 0; i < analysisVO.getAnalysis().getKeyFindings().size(); i++) {
                            analysisBuilder.append((i + 1)).append(". ").append(analysisVO.getAnalysis().getKeyFindings().get(i)).append("\n");
                        }
                        analysisBuilder.append("\n");
                    }
                    
                    if (analysisVO.getAnalysis().getSuggestions() != null && !analysisVO.getAnalysis().getSuggestions().isEmpty()) {
                        analysisBuilder.append("**优化建议：\n");
                        for (int i = 0; i < analysisVO.getAnalysis().getSuggestions().size(); i++) {
                            analysisBuilder.append((i + 1)).append(". ").append(analysisVO.getAnalysis().getSuggestions().get(i)).append("\n");
                        }
                    }
                    
                    response.setAnalysisResult(analysisBuilder.toString());
                    response.setStatus("COMPLETED");
                } catch (Exception e) {
                    log.error("解析图表结果失败", e);
                    response.setStatus("ERROR");
                }
            }
        }

        return response;
    }

    private ChartResultResponse buildChartResultFromJson(String resultJson) {
        ChartResultResponse response = new ChartResultResponse();
        response.setStatus("COMPLETED");
        
        try {
            AiAnalysisVO analysisVO = objectMapper.readValue(resultJson, AiAnalysisVO.class);
            response.setChartConfig(analysisVO.getChartConfig());
            
            // 转换 ECharts 配置字段名大小写
            ObjectNode chartConfig = (ObjectNode) objectMapper.valueToTree(analysisVO.getChartConfig());
            if (chartConfig.has("xaxis")) {
                chartConfig.set("xAxis", chartConfig.get("xaxis"));
                chartConfig.remove("xaxis");
            }
            if (chartConfig.has("yaxis")) {
                chartConfig.set("yAxis", chartConfig.get("yaxis"));
                chartConfig.remove("yaxis");
            }
            String chartConfigJson = objectMapper.writeValueAsString(chartConfig);
            response.setEchartsCode(chartConfigJson);
            
            StringBuilder analysisBuilder = new StringBuilder();
            analysisBuilder.append("**整体概况：\n").append(analysisVO.getAnalysis().getSummary()).append("\n\n");
            
            if (analysisVO.getAnalysis().getKeyFindings() != null && !analysisVO.getAnalysis().getKeyFindings().isEmpty()) {
                analysisBuilder.append("**关键发现：\n");
                for (int i = 0; i < analysisVO.getAnalysis().getKeyFindings().size(); i++) {
                    analysisBuilder.append((i + 1)).append(". ").append(analysisVO.getAnalysis().getKeyFindings().get(i)).append("\n");
                }
                analysisBuilder.append("\n");
            }
            
            if (analysisVO.getAnalysis().getSuggestions() != null && !analysisVO.getAnalysis().getSuggestions().isEmpty()) {
                analysisBuilder.append("**优化建议：\n");
                for (int i = 0; i < analysisVO.getAnalysis().getSuggestions().size(); i++) {
                    analysisBuilder.append((i + 1)).append(". ").append(analysisVO.getAnalysis().getSuggestions().get(i)).append("\n");
                }
            }
            
            response.setAnalysisResult(analysisBuilder.toString());
        } catch (Exception e) {
            log.error("构建图表结果失败", e);
            response.setStatus("ERROR");
        }
        
        return response;
    }
}



