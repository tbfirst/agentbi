package com.tbfirst.agentbiinit.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.redisson.Redisson;
/**
 *  redisson 配置类
 */
@Configuration
public class RedissonConfig {
    /**
     * 配置 redisson 客户端
     */
    @Bean
    public RedissonClient redissonClient() {
        // 创建 Config 对象
        Config config = new Config();
        // 配置 Redis 地址和密码，由于我 docker 容器端口已映射到宿主机，所以这里配置的是宿主机回环地址+端口
        config.useSingleServer().setAddress("redis://127.0.0.1:6379").setPassword("123456").setDatabase(2);
        // 返回刚创建的 Redisson 客户端
        return Redisson.create(config);
    }
}
