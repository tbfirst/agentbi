package com.tbfirst.agentbiinit.config;

import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RBloomFilter;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;
import java.util.concurrent.CompletableFuture;

@Configuration
@Slf4j
public class BloomFilterConfig {

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Bean
    public RBloomFilter<String> chartBloomFilter(RedissonClient redisson) {
        RBloomFilter<String> bloomFilter = redisson.getBloomFilter("chart:bloom");

        // 预期100万数据，误判率0.1%
        bloomFilter.tryInit(1000000L, 0.001);

        // todo 异步预热，避免阻塞启动
        CompletableFuture.runAsync(() -> warmUpBloomFilter(bloomFilter));

        return bloomFilter;
    }

    private void warmUpBloomFilter(RBloomFilter<String> bloomFilter) {
        // 自行实现异步的预热逻辑，从数据库查询近期活跃指纹并添加到布隆过滤器中
    }
}
