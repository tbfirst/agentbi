package com.tbfirst.agentbiinit.service.impl;

import com.tbfirst.agentbiinit.mapper.ChartMapper;
import com.tbfirst.agentbiinit.model.entity.ChartEntity;
import com.tbfirst.agentbiinit.service.BloomFilterAsyncService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RBloomFilter;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * 布隆过滤器异步服务实现类
 * 
 * 功能说明：
 * 1. 异步添加指纹到布隆过滤器，避免阻塞主线程
 * 2. 支持批量添加，提高性能
 * 3. 支持从数据库预热历史数据
 * 4. 支持定期重建布隆过滤器，清理过期数据
 * 
 * 使用场景：
 * - 用户上传文件生成指纹后，异步添加到布隆过滤器
 * - 系统启动时预热最近30天的活跃指纹
 * - 定期重建布隆过滤器，保持数据准确性
 * 
 * @author tbfirst
 */
@Service
@Slf4j
public class BloomFilterAsyncServiceImpl implements BloomFilterAsyncService {

    @Autowired
    private RedissonClient redissonClient;

    @Autowired
    private ChartMapper chartMapper;

    /**
     * 布隆过滤器预期插入数据量，默认100万
     */
    @Value("${bloom.filter.expected-insertions:1000000}")
    private long expectedInsertions;

    /**
     * 布隆过滤器误判率，默认0.1%
     */
    @Value("${bloom.filter.false-probability:0.001}")
    private double falseProbability;

    /**
     * 预热数据的天数范围，默认预热最近30天的数据
     */
    @Value("${bloom.filter.warm-up-days:30}")
    private int warmUpDays;

    /**
     * 布隆过滤器Redis Key名称
     */
    private static final String BLOOM_FILTER_NAME = "chart:bloom:v2";
    
    /**
     * 布隆过滤器计数器Key，用于统计当前插入数量
     */
    private static final String BLOOM_FILTER_COUNT_KEY = "chart:bloom:count";
    
    /**
     * 布隆过滤器元数据Key，用于记录最后更新时间等信息
     */
    private static final String BLOOM_FILTER_META_KEY = "chart:bloom:meta";

    /**
     * 布隆过滤器实例
     */
    private RBloomFilter<String> bloomFilter;

    /**
     * 初始化布隆过滤器
     * 在Bean创建后自动执行，如果布隆过滤器不存在则创建
     */
    @PostConstruct      // 该注释用于在Bean创建后自动执行初始化方法
    public void init() {
        bloomFilter = redissonClient.getBloomFilter(BLOOM_FILTER_NAME);
        
        if (!bloomFilter.isExists()) {
            log.info("布隆过滤器不存在，开始初始化，预期数据量: {}, 误判率: {}", expectedInsertions, falseProbability);
            // 初始化布隆过滤器，设置预期插入数据量和误判率
            bloomFilter.tryInit(expectedInsertions, falseProbability);
            log.info("布隆过滤器初始化完成");
        } else {
            log.info("布隆过滤器已存在，跳过初始化");
        }
    }

    /**
     * 异步添加单个key到布隆过滤器
     * 使用@Async注解在独立线程池中执行，不阻塞主线程
     * 
     * @param key 要添加的指纹key
     */
    @Override
    @Async("lightTaskExecutor")
    public void asyncAddToBloomFilter(String key) {
        if (key == null || key.isEmpty()) {
            log.warn("尝试添加空key到布隆过滤器，已忽略");
            return;
        }

        try {
            // 先检查是否已存在，避免重复计数
            if (!bloomFilter.contains(key)) {
                bloomFilter.add(key);
                // 更新计数器
                redissonClient.getAtomicLong(BLOOM_FILTER_COUNT_KEY).incrementAndGet();
                log.debug("异步添加key到布隆过滤器: {}", key);
            } else {
                log.debug("key已存在于布隆过滤器中，跳过: {}", key);
            }
        } catch (Exception e) {
            log.error("异步添加布隆过滤器失败, key: {}", key, e);
        }
    }

    /**
     * 异步批量添加key到布隆过滤器
     * 适用于系统启动时预热大量数据的场景
     * 
     * @param keys 要添加的指纹key列表
     */
    @Override
    @Async("lightTaskExecutor")
    public void asyncAddToBloomFilterBatch(List<String> keys) {
        if (keys == null || keys.isEmpty()) {
            log.warn("尝试批量添加空列表到布隆过滤器，已忽略");
            return;
        }

        try {
            int addedCount = 0;
            int skippedCount = 0;
            
            for (String key : keys) {
                if (key != null && !key.isEmpty()) {
                    if (!bloomFilter.contains(key)) {
                        bloomFilter.add(key);
                        addedCount++;
                    } else {
                        skippedCount++;
                    }
                }
            }
            
            // 更新计数器
            if (addedCount > 0) {
                redissonClient.getAtomicLong(BLOOM_FILTER_COUNT_KEY).addAndGet(addedCount);
            }
            
            log.info("批量异步添加完成，新增: {}，跳过: {}，总计: {}", addedCount, skippedCount, keys.size());
        } catch (Exception e) {
            log.error("批量异步添加布隆过滤器失败", e);
        }
    }

    /**
     * 异步查询key是否可能存在于布隆过滤器中
     * 注意：布隆过滤器可能存在误判，但不会漏判
     * 
     * @param key 要查询的指纹key
     * @return CompletableFuture包装的查询结果，true表示可能存在，false表示一定不存在
     */
    @Override
    public CompletableFuture<Boolean> mightContain(String key) {
        // 使用supplyAsync在独立线程池中执行查询，不阻塞主线程
        return CompletableFuture.supplyAsync(() -> {
            if (key == null || key.isEmpty()) {
                return false;
            }
            try {
                return bloomFilter.contains(key);
            } catch (Exception e) {
                log.error("检查布隆过滤器失败, key: {}", key, e);
                return false;
            }
        });
    }

    /**
     * 从数据库预热布隆过滤器
     * 查询最近N天的活跃指纹数据并添加到布隆过滤器
     * 通常在系统启动时或定时任务中调用
     */
    @Override
    @Async("lightTaskExecutor")
    public void warmUpFromDatabase() {
        log.info("开始从数据库预热布隆过滤器...");
        long startTime = System.currentTimeMillis();

        try {
            // 计算预热数据的起始时间
            LocalDateTime warmUpThreshold = LocalDateTime.now().minusDays(warmUpDays);
            
            // 构建查询条件：只查询最近N天且有指纹的记录
            QueryWrapper<ChartEntity> queryWrapper = new QueryWrapper<>();
            queryWrapper.select("fingerprint", "created_time")
                    .ge("created_time", warmUpThreshold)
                    .isNotNull("fingerprint");

            List<ChartEntity> charts = chartMapper.selectList(queryWrapper);
            
            if (charts.isEmpty()) {
                log.info("没有需要预热的数据");
                return;
            }

            // 批量添加到布隆过滤器
            int addedCount = 0;
            int skippedCount = 0;
            
            for (ChartEntity chart : charts) {
                String fingerprint = chart.getFingerprint();
                if (fingerprint != null && !fingerprint.isEmpty()) {
                    if (!bloomFilter.contains(fingerprint)) {
                        bloomFilter.add(fingerprint);
                        addedCount++;
                    } else {
                        skippedCount++;
                    }
                }
            }

            // 更新计数器
            if (addedCount > 0) {
                redissonClient.getAtomicLong(BLOOM_FILTER_COUNT_KEY).addAndGet(addedCount);
            }

            long costTime = System.currentTimeMillis() - startTime;
            log.info("布隆过滤器预热完成，查询: {}，新增: {}，跳过: {}，耗时: {}ms", 
                    charts.size(), addedCount, skippedCount, costTime);

            // 更新元数据
            updateMeta("warm_up", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));

        } catch (Exception e) {
            log.error("布隆过滤器预热失败", e);
        }
    }

    /**
     * 重建布隆过滤器
     * 适用于以下场景：
     * 1. 布隆过滤器数据量过大，需要清理
     * 2. 误判率过高，需要重新设置参数
     * 3. 数据大量过期，需要重建
     * 
     * 重建过程：
     * 1. 创建新的布隆过滤器
     * 2. 从数据库加载所有有效指纹
     * 3. 批量添加到新布隆过滤器
     * 4. 原子性替换旧布隆过滤器
     */
    @Override
    public void rebuildBloomFilter() {
        log.info("开始重建布隆过滤器...");
        long startTime = System.currentTimeMillis();

        try {
            // 创建新的布隆过滤器
            String newFilterName = BLOOM_FILTER_NAME + ":new:" + System.currentTimeMillis();
            RBloomFilter<String> newFilter = redissonClient.getBloomFilter(newFilterName);
            newFilter.tryInit(expectedInsertions, falseProbability);

            // 从数据库加载所有有效指纹
            QueryWrapper<ChartEntity> queryWrapper = new QueryWrapper<>();
            queryWrapper.select("fingerprint").isNotNull("fingerprint");
            List<ChartEntity> charts = chartMapper.selectList(queryWrapper);

            // 批量添加到新布隆过滤器
            int addedCount = 0;
            for (ChartEntity chart : charts) {
                String fingerprint = chart.getFingerprint();
                if (fingerprint != null && !fingerprint.isEmpty()) {
                    newFilter.add(fingerprint);
                    addedCount++;
                }
            }

            // 删除旧布隆过滤器
            redissonClient.getBloomFilter(BLOOM_FILTER_NAME).delete();
            
            // 重命名新布隆过滤器
            newFilter.rename(BLOOM_FILTER_NAME);
            
            // 更新本地引用
            bloomFilter = redissonClient.getBloomFilter(BLOOM_FILTER_NAME);

            // 重置计数器
            redissonClient.getAtomicLong(BLOOM_FILTER_COUNT_KEY).set(addedCount);

            long costTime = System.currentTimeMillis() - startTime;
            log.info("布隆过滤器重建完成，数据量: {}，耗时: {}ms", addedCount, costTime);

            // 更新元数据
            updateMeta("rebuild", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));

        } catch (Exception e) {
            log.error("布隆过滤器重建失败", e);
        }
    }

    /**
     * 获取布隆过滤器的预期插入数量
     * @return 预期插入数量配置值
     */
    @Override
    public long getExpectedInsertions() {
        return expectedInsertions;
    }

    /**
     * 获取布隆过滤器的当前键数量
     * 注意：这是近似值，实际数量可能略有偏差
     * @return 当前已插入的键数量
     */
    @Override
    public long getCurrentCount() {
        return redissonClient.getAtomicLong(BLOOM_FILTER_COUNT_KEY).get();
    }

    /**
     * 获取布隆过滤器的假阳性概率
     * @return 配置的假阳性概率
     */
    @Override
    public double getFalseProbability() {
        return falseProbability;
    }

    /**
     * 更新布隆过滤器元数据
     * 记录最后操作类型和时间，便于运维监控
     * 
     * @param operation 操作类型（warm_up/rebuild等）
     * @param timestamp 操作时间戳
     */
    private void updateMeta(String operation, String timestamp) {
        try {
            String metaValue = operation + ":" + timestamp;
            redissonClient.getBucket(BLOOM_FILTER_META_KEY).set(metaValue);
        } catch (Exception e) {
            log.error("更新布隆过滤器元数据失败", e);
        }
    }
}
