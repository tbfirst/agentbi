package com.tbfirst.agentbiinit.service;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public interface BloomFilterAsyncService {
    /**
     * 异步添加键到布隆过滤器
     * @param key 要添加的键
     */
    void asyncAddToBloomFilter(String key);

    /**
     * 异步批量添加键到布隆过滤器
     * @param keys 要添加的键列表
     */
    void asyncAddToBloomFilterBatch(List<String> keys);
    
    /**
     * 异步查询键是否可能包含在布隆过滤器中
     * @param key 要查询的键
     * @return 一个 CompletableFuture，用于异步获取查询结果
     */
    CompletableFuture<Boolean> mightContain(String key);

    /**
     * 异步从数据库中预热布隆过滤器
     */
    void warmUpFromDatabase();

    /**
     * 异步重建布隆过滤器
     */
    void rebuildBloomFilter();

    /**
     * 异步获取布隆过滤器的预期插入数量
     * @return 布隆过滤器的预期插入数量
     */
    long getExpectedInsertions();

    /**
     * 异步获取布隆过滤器的当前键数量
     * @return 布隆过滤器的当前键数量
     */
    long getCurrentCount();

    /**
     * 异步获取布隆过滤器的假阳性概率
     * @return 布隆过滤器的假阳性概率
     */
    double getFalseProbability();
}
