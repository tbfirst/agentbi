package com.tbfirst.agentbiinit.service.impl;

import com.tbfirst.agentbiinit.service.TimeShardingCleanupService;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RKeys;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static com.tbfirst.agentbiinit.model.enums.AiRedisEnum.*;

/**
 * 时间分片清理服务实现类
 * @author tbfirst
 */
@Service
@Slf4j
public class TimeShardingCleanupServiceImpl implements TimeShardingCleanupService {

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private RedissonClient redissonClient;

    @Value("${cache.cleanup.retention-days:30}")
    private int defaultRetentionDays;

    @Value("${cache.cleanup.batch-size:1000}")
    private int batchSize;

    @Value("${cache.archive.days:90}")
    private int archiveDays;

    private static final String SHARD_PREFIX = "shard:";
    private static final String SHARD_INDEX_KEY = "shard:index";
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final DateTimeFormatter MONTH_FORMATTER = DateTimeFormatter.ofPattern("yyyyMM");

    @Override
    @Scheduled(cron = "${cache.cleanup.cron:0 0 2 * * ?}")          //每天凌晨2点执行
    public void cleanExpiredData() {
        log.info("开始清理过期数据...");
        long startTime = System.currentTimeMillis();

        try {
            cleanExpiredDataByType(CHART_RESULT.getValue(), defaultRetentionDays);
            cleanExpiredDataByType(CHART_HISTORY.getValue(), defaultRetentionDays);
            cleanExpiredDataByType(CHART_TASK.getValue(), 7);
            cleanExpiredDataByType(FILE_TASK.getValue(), 7);

            cleanExpiredShards();

            long costTime = System.currentTimeMillis() - startTime;
            log.info("过期数据清理完成，耗时: {}ms", costTime);

        } catch (Exception e) {
            log.error("清理过期数据失败", e);
        }
    }

    @Override
    public void cleanExpiredDataByType(String dataType, int retentionDays) {
        log.info("清理类型 {} 的过期数据，保留天数: {}", dataType, retentionDays);

        try {
            Set<String> keys = redisTemplate.keys(dataType + "*");
            if (keys == null || keys.isEmpty()) {
                log.info("类型 {} 没有需要清理的数据", dataType);
                return;
            }

            int deletedCount = 0;
            List<String> keysToDelete = new ArrayList<>();

            for (String key : keys) {
                try {
                    Long expireTime = redisTemplate.getExpire(key, TimeUnit.SECONDS);
                    if (expireTime != null && expireTime < 0) {
                        keysToDelete.add(key);
                    }

                    if (keysToDelete.size() >= batchSize) {
                        redisTemplate.delete(keysToDelete);
                        deletedCount += keysToDelete.size();
                        keysToDelete.clear();
                        log.debug("批量删除 {} 个过期key", batchSize);
                    }
                } catch (Exception e) {
                    log.warn("处理key失败: {}", key, e);
                }
            }

            if (!keysToDelete.isEmpty()) {
                redisTemplate.delete(keysToDelete);
                deletedCount += keysToDelete.size();
            }

            log.info("类型 {} 清理完成，删除 {} 个key", dataType, deletedCount);

        } catch (Exception e) {
            log.error("清理类型 {} 的过期数据失败", dataType, e);
        }
    }

    @Override
    @Scheduled(cron = "${cache.archive.cron:0 0 3 1 * ?}")
    public void archiveOldData() {
        log.info("开始归档旧数据，保留档天数: {}", archiveDays);
        long startTime = System.currentTimeMillis();

        try {
            LocalDateTime archiveThreshold = LocalDateTime.now().minusDays(archiveDays);
            String archiveShardKey = SHARD_PREFIX + "archive:" + archiveThreshold.format(MONTH_FORMATTER);

            Set<String> allKeys = redisTemplate.keys("chart:*");
            if (allKeys == null || allKeys.isEmpty()) {
                log.info("没有需要归档的数据");
                return;
            }

            int archivedCount = 0;
            for (String key : allKeys) {
                try {
                    Object value = redisTemplate.opsForValue().get(key);
                    if (value != null) {
                        String archiveKey = archiveShardKey + ":" + key;
                        redisTemplate.opsForValue().set(archiveKey, value, Duration.ofDays(365));
                        archivedCount++;
                    }
                } catch (Exception e) {
                    log.warn("归档key失败: {}", key, e);
                }
            }

            long costTime = System.currentTimeMillis() - startTime;
            log.info("旧数据归档完成，归档 {} 个key，耗时: {}ms", archivedCount, costTime);

        } catch (Exception e) {
            log.error("归档旧数据失败", e);
        }
    }

    @Override
    public void migrateToCurrentShard(String key, Object value) {
        String currentShardKey = getCurrentShardKey(key);
        redisTemplate.opsForValue().set(currentShardKey, value, Duration.ofDays(defaultRetentionDays));
        
        updateShardIndex(currentShardKey);
        
        log.debug("数据迁移到当前分片: {} -> {}", key, currentShardKey);
    }

    @Override
    public String getCurrentShardKey(String baseKey) {
        String today = LocalDateTime.now().format(DATE_FORMATTER);
        return SHARD_PREFIX + today + ":" + baseKey;
    }

    @Override
    public String getShardKeyByDate(String baseKey, LocalDateTime date) {
        String dateStr = date.format(DATE_FORMATTER);
        return SHARD_PREFIX + dateStr + ":" + baseKey;
    }

    @Override
    public List<String> getShardKeysInRange(String baseKey, LocalDateTime start, LocalDateTime end) {
        List<String> shardKeys = new ArrayList<>();
        LocalDateTime current = start;

        while (!current.isAfter(end)) {
            String shardKey = getShardKeyByDate(baseKey, current);
            shardKeys.add(shardKey);
            current = current.plusDays(1);
        }

        return shardKeys;
    }

    @Override
    public long getDataSizeByShard(String shardKey) {
        Set<String> keys = redisTemplate.keys(shardKey + "*");
        return keys != null ? keys.size() : 0;
    }

    @Override
    @Scheduled(cron = "${cache.rebuild-index.cron:0 0 4 * * ?}")
    public void rebuildShardIndex() {
        log.info("开始重建分片索引...");
        long startTime = System.currentTimeMillis();

        try {
            RKeys keys = redissonClient.getKeys();
            Iterable<String> allKeys = keys.getKeysByPattern(SHARD_PREFIX + "*");

            redissonClient.getSet(SHARD_INDEX_KEY).delete();

            int indexCount = 0;
            for (String key : allKeys) {
                String shardId = extractShardId(key);
                if (shardId != null) {
                    redissonClient.getSet(SHARD_INDEX_KEY).add(shardId);
                    indexCount++;
                }
            }

            long costTime = System.currentTimeMillis() - startTime;
            log.info("分片索引重建完成，索引数: {}，耗时: {}ms", indexCount, costTime);

        } catch (Exception e) {
            log.error("重建分片索引失败", e);
        }
    }

    private void cleanExpiredShards() {
        log.info("清理过期分片...");

        try {
            LocalDateTime threshold = LocalDateTime.now().minusDays(defaultRetentionDays);
            String thresholdShardId = threshold.format(DATE_FORMATTER);

            Set<Object> shardIndex = redissonClient.getSet(SHARD_INDEX_KEY).readAll();
            if (shardIndex == null || shardIndex.isEmpty()) {
                return;
            }

            int deletedShards = 0;
            for (Object shardId : shardIndex) {
                String shardIdStr = shardId.toString();
                if (shardIdStr.compareTo(thresholdShardId) < 0) {
                    Set<String> keysToDelete = redisTemplate.keys(SHARD_PREFIX + shardIdStr + "*");
                    if (keysToDelete != null && !keysToDelete.isEmpty()) {
                        redisTemplate.delete(keysToDelete);
                        deletedShards++;
                    }
                    redissonClient.getSet(SHARD_INDEX_KEY).remove(shardId);
                }
            }

            log.info("过期分片清理完成，删除 {} 个分片", deletedShards);

        } catch (Exception e) {
            log.error("清理过期分片失败", e);
        }
    }

    private void updateShardIndex(String shardKey) {
        String shardId = extractShardId(shardKey);
        if (shardId != null) {
            redissonClient.getSet(SHARD_INDEX_KEY).add(shardId);
        }
    }

    private String extractShardId(String key) {
        if (key == null || !key.startsWith(SHARD_PREFIX)) {
            return null;
        }

        String[] parts = key.split(":");
        if (parts.length >= 2) {
            return parts[1];
        }

        return null;
    }
}
