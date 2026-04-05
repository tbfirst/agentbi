package com.tbfirst.agentbiinit.service;

import java.time.LocalDateTime;
import java.util.List;

public interface TimeShardingCleanupService {
    /**
     * 清理过期数据
     */
    void cleanExpiredData();
    
    /**
     * 清理指定数据类型的过期数据
     */
    void cleanExpiredDataByType(String dataType, int retentionDays);
    
    /**
     * 归档旧数据
     */
    void archiveOldData();
    
    /**
     * 迁移数据到当前分片
     * @param key 数据键
     * @param value 数据值
     */
    void migrateToCurrentShard(String key, Object value);

    /**
     * 获取当前分片键
     * @param baseKey
     * @return
     */
    String getCurrentShardKey(String baseKey);

    /**
     * 根据日期获取分片键
     * @param baseKey
     * @param date
     * @return
     */
    String getShardKeyByDate(String baseKey, LocalDateTime date);

    /**
     * 获取指定时间范围内的分片键
     * @param baseKey
     * @param start
     * @param end
     * @return
     */
    List<String> getShardKeysInRange(String baseKey, LocalDateTime start, LocalDateTime end);

    /**
     * 获取指定分片的数据量
     * @param shardKey
     * @return
     */
    long getDataSizeByShard(String shardKey);
    
    /**
     * 重建构建分片索引
     */
    void rebuildShardIndex();

}
