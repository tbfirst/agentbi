package com.tbfirst.agentbiinit.config;

import lombok.extern.slf4j.Slf4j;
import org.redisson.Redisson;
import org.redisson.api.NatMapper;
import org.redisson.api.RedissonClient;
import org.redisson.config.ClusterServersConfig;
import org.redisson.config.Config;
import org.redisson.config.ReadMode;
import org.redisson.config.SingleServerConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import java.util.Arrays;

@Configuration
@Slf4j
public class RedissonConfig {

    @Value("${spring.data.redis.host:127.0.0.1}")
    private String host;

    @Value("${spring.data.redis.port:6379}")
    private int port;

    @Value("${spring.data.redis.password:}")
    private String password;

    @Value("${spring.data.redis.database:0}")
    private int database;

    @Value("${spring.data.redis.cluster.nodes:}")
    private String clusterNodes;

    @Value("${spring.data.redis.cluster.max-redirects:3}")
    private int maxRedirects;

    @Bean
    @Primary
    // @ConditionalOnProperty 用于根据配置属性的值来判断是否创建该 Bean
    // 此处是通过配置属性 spring.data.redis.cluster.enabled 来判断是否创建该 Bean
    // havingValue = "true" 表示如果配置属性的值为 true，则创建该 Bean
    @ConditionalOnProperty(name = "spring.data.redis.cluster.enabled", havingValue = "true", matchIfMissing = false)
    public RedissonClient redissonClusterClient() {
        log.info("初始化Redisson集群客户端...");

        // 创建 Config 对象
        Config config = new Config();
        // 配置集群服务器，即 Redis 集群模式下的节点
        ClusterServersConfig clusterConfig = config.useClusterServers();

        if (password != null && !password.isEmpty()) {
            clusterConfig.setPassword(password);
        }
        // 配置扫描间隔，即 Redisson 定期扫描集群节点的时间间隔
        clusterConfig.setScanInterval(5000);
        // 配置连接超时时间，即 Redisson 连接集群节点的超时时间
        clusterConfig.setConnectTimeout(10000);
        // 配置超时时间，即 Redisson 操作超时时间
        clusterConfig.setTimeout(3000);
        // 配置重试次数，即 Redisson 连接失败后的重试次数
        clusterConfig.setRetryAttempts(maxRedirects);
        // 配置重试间隔，即 Redisson 重试之间的间隔时间
        clusterConfig.setRetryInterval(1500);

        // 配置从节点同步超时时间，即 Redisson 等待从节点同步数据的超时时间
        config.setSlavesSyncTimeout(30000);
        config.setCheckLockSyncedSlaves(false);    // 禁用锁的从节点同步检查（开发环境）
        clusterConfig.setReadMode(ReadMode.SLAVE); // 读取模式：优先从节点读取数据

        if (clusterNodes != null && !clusterNodes.isEmpty()) {
            String[] nodes = clusterNodes.split(",");
            // 配置集群节点地址，格式为 redis://<host:port>
            String[] addresses = Arrays.stream(nodes)
                    .map(String::trim)
                    .map(node -> "redis://" + node)
                    .toArray(String[]::new);
            clusterConfig.addNodeAddress(addresses);
            log.info("Redisson集群客户端配置完成，节点数: {}", addresses.length);
        } else {
            // 配置默认集群节点地址
            String[] defaultNodes = {
                    "redis://127.0.0.1:7000",
                    "redis://127.0.0.1:7001",
                    "redis://127.0.0.1:7002",
                    "redis://127.0.0.1:7003",
                    "redis://127.0.0.1:7004",
                    "redis://127.0.0.1:7005"
            };
            clusterConfig.addNodeAddress(defaultNodes);
            log.info("Redisson集群客户端使用默认节点配置");
        }

        return Redisson.create(config);     // 创建 RedissonClient 实例并返回
    }

    @Bean
    // @ConditionalOnProperty 用于根据配置属性的值来判断是否创建该 Bean
    // 此处是通过配置属性 spring.data.redis.cluster.enabled 来判断是否创建该 Bean
    // havingValue = "false" 表示如果配置属性的值为 false，则创建该 Bean
    @ConditionalOnProperty(name = "spring.data.redis.cluster.enabled", havingValue = "false", matchIfMissing = true)
    public RedissonClient redissonSingleClient() {
        log.info("初始化Redisson单机客户端...");

        // 同理创建单机客户端配置
        Config config = new Config();
        SingleServerConfig singleConfig = config.useSingleServer();

        String address = "redis://" + host + ":" + port;
        singleConfig.setAddress(address);
        singleConfig.setDatabase(database);

        if (password != null && !password.isEmpty()) {
            singleConfig.setPassword(password);
        }

        singleConfig.setConnectTimeout(10000);
        singleConfig.setTimeout(3000);
        singleConfig.setRetryAttempts(3);
        singleConfig.setRetryInterval(1500);
        singleConfig.setConnectionPoolSize(64);
        singleConfig.setConnectionMinimumIdleSize(10);

        log.info("Redisson单机客户端配置完成，地址: {}", address);

        return Redisson.create(config);
    }

}
