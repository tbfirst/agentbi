package com.tbfirst.agentbiinit.config;

import org.redisson.api.RBloomFilter;
import org.redisson.api.RedissonClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class BloomFilterConfig {

    @Bean
    public RBloomFilter<String> fileBloomFilter(RedissonClient redisson) {
        RBloomFilter<String> filter = redisson.getBloomFilter("file:bloom");
        // 预期数据量100万，误判率0.1%
        filter.tryInit(1000000L, 0.001);
        return filter;
    }
}
