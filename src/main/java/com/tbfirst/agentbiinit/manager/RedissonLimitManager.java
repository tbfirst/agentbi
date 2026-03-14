package com.tbfirst.agentbiinit.manager;

import org.redisson.api.RRateLimiter;
import org.redisson.api.RateIntervalUnit;
import org.redisson.api.RateType;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Redisson限流器管理类
 */
@Component
public class RedissonLimitManager {
    @Autowired
    private RedissonClient redissonClient;

    /**
     * 初始化限流器
     * @param limitKey      限流键（建议按用户、业务等区分）
     * @param rate          令牌个数
     * @param bucketSize    令牌桶容量
     */
    public void initRateLimiter(String limitKey, int rate, int bucketSize) {
        // 通过唯一的限流键获取对应限流器
        RRateLimiter rateLimiter = redissonClient.getRateLimiter(limitKey);

        // 初始化：每秒生成 rate 个令牌，桶容量为 bucketSize
        rateLimiter.trySetRate(RateType.OVERALL, rate, bucketSize, RateIntervalUnit.SECONDS);
    }

    /**
     * 检查是否超过限流阈值
     * @param limitKey 限流键
     * @return 是否超过限流阈值
     */
    public boolean isOverLimit(String limitKey) {
        // 通过唯一的限流键获取对应限流器
        RRateLimiter rateLimiter = redissonClient.getRateLimiter(limitKey);

        // 尝试获取一个令牌，若无法获取（即超过限流阈值），则返回 true
        return !rateLimiter.tryAcquire();
    }
}
