package com.tbfirst.agentbiinit.config;

import io.lettuce.core.cluster.ClusterClientOptions;
import io.lettuce.core.cluster.ClusterTopologyRefreshOptions;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisClusterConfiguration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisNode;
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Configuration
@EnableCaching  // 开启缓存支持
@Slf4j
public class RedisClusterConfig {

    @Value("${spring.data.redis.cluster.nodes:}")
    private String clusterNodes;

    @Value("${spring.data.redis.cluster.max-redirects:3}")
    private int maxRedirects;

    @Value("${spring.data.redis.password:}")
    private String password;

    @Value("${spring.data.redis.timeout:3000}")
    private long timeout;

    @Value("${spring.data.redis.lettuce.pool.max-active:8}")
    private int maxActive;

    @Value("${spring.data.redis.lettuce.pool.max-idle:8}")
    private int maxIdle;

    @Value("${spring.data.redis.lettuce.pool.min-idle:0}")
    private int minIdle;

    /**
     * 配置Redis分片集群连接工厂
     */
    @Bean
    @Primary
    // 如果配置属性 spring.data.redis.cluster.enabled 的值为 true，则创建该 Bean
    @ConditionalOnProperty(name = "spring.data.redis.cluster.enabled", havingValue = "true")
    public LettuceConnectionFactory redisClusterConnectionFactory() {
        log.info("初始化Redis分片集群连接工厂...");

        List<RedisNode> nodes = parseClusterNodes(clusterNodes);

        // 配置Redis集群连接工厂
        RedisClusterConfiguration clusterConfig = new RedisClusterConfiguration();
        clusterConfig.setClusterNodes(nodes);
        clusterConfig.setMaxRedirects(maxRedirects);
        if (password != null && !password.isEmpty()) {
            clusterConfig.setPassword(password);
        }

        // 配置Redis集群连接工厂的拓扑刷新选项
        // 简单来说，就是每10分钟刷新一次集群的节点信息，包括节点的状态、节点的连接数等
        ClusterTopologyRefreshOptions topologyRefreshOptions = ClusterTopologyRefreshOptions.builder()
                .enablePeriodicRefresh(Duration.ofMinutes(10))
                .enableAllAdaptiveRefreshTriggers()
                .build();

        // 通过拓扑刷新选项配置客户端选项，同时开启自动重连，当连接断开时，会自动重连到其他节点，确保高可用
        ClusterClientOptions clientOptions = ClusterClientOptions.builder()
                .topologyRefreshOptions(topologyRefreshOptions)
                .autoReconnect(true)
                .build();

        // 通过客户端选项配置Lettuce客户端配置
        LettuceClientConfiguration clientConfig = LettuceClientConfiguration.builder()
                .commandTimeout(Duration.ofMillis(timeout))
                .clientOptions(clientOptions)
                .build();

        // 配置Lettuce客户端连接工厂并开启共享连接和验证连接功能
        LettuceConnectionFactory factory = new LettuceConnectionFactory(clusterConfig, clientConfig);
        factory.setShareNativeConnection(true);
        factory.setValidateConnection(true);
        
        log.info("Redis分片集群连接工厂初始化完成，节点数: {}", nodes.size());
        return factory;
    }

    /**
     * 解析Redis集群节点为RedisNode列表
     * @param nodes
     * @return
     */
    private List<RedisNode> parseClusterNodes(String nodes) {
        if (nodes == null || nodes.isEmpty()) {
            log.warn("未配置Redis集群节点，使用默认配置");
            return Arrays.asList(
                    new RedisNode("127.0.0.1", 7000),
                    new RedisNode("127.0.0.1", 7001),
                    new RedisNode("127.0.0.1", 7002),
                    new RedisNode("127.0.0.1", 7003),
                    new RedisNode("127.0.0.1", 7004),
                    new RedisNode("127.0.0.1", 7005)
            );
        }

        // 如果有单独配置的节点，则使用配置的节点，否则使用默认配置
        return Arrays.stream(nodes.split(","))
                .map(String::trim)
                .map(node -> {
                    String[] parts = node.split(":");   // host:port
                    return new RedisNode(parts[0], Integer.parseInt(parts[1]));
                })
                .collect(Collectors.toList());
    }

    /**
     * 配置自定义RedisTemplate序列化器
     * 使用GenericJackson2JsonRedisSerializer替代已弃用的Jackson2JsonRedisSerializer
     * GenericJackson2JsonRedisSerializer会自动在JSON中包含类型信息，支持多态类型
     * 
     * @param connectionFactory Redis连接工厂
     * @return 配置好的RedisTemplate实例
     */
    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);

        // 使用GenericJackson2JsonRedisSerializer进行JSON序列化
        // 相比Jackson2JsonRedisSerializer，它自动包含类型信息，无需手动配置ObjectMapper
        GenericJackson2JsonRedisSerializer jsonSerializer = new GenericJackson2JsonRedisSerializer();

        // String序列化器，用于Key的序列化
        StringRedisSerializer stringRedisSerializer = new StringRedisSerializer();
        
        // Key使用String序列化
        template.setKeySerializer(stringRedisSerializer);
        template.setHashKeySerializer(stringRedisSerializer);
        
        // Value使用JSON序列化
        template.setValueSerializer(jsonSerializer);
        template.setHashValueSerializer(jsonSerializer);
        
        template.afterPropertiesSet();

        log.info("RedisTemplate初始化完成，使用GenericJackson2JsonRedisSerializer序列化器");
        return template;
    }

    @Bean
    public StringRedisTemplate stringRedisTemplate(RedisConnectionFactory connectionFactory) {
        StringRedisTemplate template = new StringRedisTemplate();
        template.setConnectionFactory(connectionFactory);
        log.info("StringRedisTemplate初始化完成");
        return template;
    }

    /**
     * 配置Spring Cache缓存管理器
     * 使用GenericJackson2JsonRedisSerializer替代已弃用的Jackson2JsonRedisSerializer
     * GenericJackson2JsonRedisSerializer会自动在JSON中包含类型信息，无需手动配置ObjectMapper
     */
    @Bean
    public CacheManager cacheManager(RedisConnectionFactory connectionFactory) {
        GenericJackson2JsonRedisSerializer jsonSerializer = new GenericJackson2JsonRedisSerializer();

        RedisCacheConfiguration config = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofHours(2))
                .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(jsonSerializer))
                .disableCachingNullValues()
                .prefixCacheNameWith("bi:cache:");

        RedisCacheManager cacheManager = RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(config)
                .transactionAware()
                .build();
        
        log.info("CacheManager初始化完成");
        return cacheManager;
    }
}
