package com.tbfirst.agentbiinit.config;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RBloomFilter;
import org.redisson.api.RedissonClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@Slf4j
public class BloomFilterConfig {

    @Bean
    public RBloomFilter<String> chartBloomFilter(RedissonClient redisson) {
        RBloomFilter<String> bloomFilter = redisson.getBloomFilter("chart:bloom:v2");

        if (!bloomFilter.isExists()) {
            log.info("初始化布隆过滤器...");
            // 预期100万数据，误判率0.1%
            bloomFilter.tryInit(1000000L, 0.001);
        }

        log.info("布隆过滤器配置完成，异步预热将在后台执行");

        return bloomFilter;
    }
}
