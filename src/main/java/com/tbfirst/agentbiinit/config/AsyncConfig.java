package com.tbfirst.agentbiinit.config;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

@EnableAsync    // ← 开启异步支持
@Configuration
public class AsyncConfig {

    /**
     * AI分析专用线程池 - 核心配置（后被 RabbitMQ 替换）
     * 特点：IO密集型、任务耗时长、需要优雅关闭
     */
    @Bean("aiAnalysisExecutor")
    public ThreadPoolTaskExecutor aiAnalysisExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        // 在容器化等环境中获取CPU核心数可能无法获取
//        int cpuCores = Runtime.getRuntime().availableProcessors();
        // IO密集型：核心线程数可以设置较大（这里设置为4，根据实际情况调整）
        executor.setCorePoolSize(16);
        // 最大线程数：根据AI服务并发限制设置（如AI服务限制10并发，这里设置8）
        executor.setMaxPoolSize(20);
        // 队列容量：不宜过大，防止任务堆积过多导致内存溢出
        // 建议：根据业务峰值设置，如每秒10请求，每个任务10秒，则队列100左右
        executor.setQueueCapacity(50);
        // 线程存活时间：60秒，快速回收空闲线程
        executor.setKeepAliveSeconds(60);
        // 线程命名前缀：便于监控和日志排查
        executor.setThreadNamePrefix("ai-analysis-");

        // 拒绝策略：CallerRunsPolicy - 让调用线程自己执行，起到限流作用
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());

        // 关闭配置
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(120); // 等待2分钟让任务完成

        // 初始化线程池，放在最后执行确保配置生效
        executor.initialize();
        return executor;
    }

    /**
     * 文件转换专用线程池 - 核心配置
     */
    @Bean("fileParserExecutor")
    public ThreadPoolTaskExecutor fileParserExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        // 核心线程数
        // 对于文件读写这类的IO密集型任务，核心线程数设置为两倍的CPU核心数+1
        executor.setCorePoolSize(16);
        // 最大线程数
        executor.setMaxPoolSize(20);
        // 队列容量：不宜过大，防止任务堆积过多导致内存溢出
        executor.setQueueCapacity(20);
        // 线程存活时间：60秒，快速回收空闲线程
        executor.setKeepAliveSeconds(60);
        // 线程命名前缀：便于监控和日志排查
        executor.setThreadNamePrefix("file-parser-");
        // 拒绝策略：CallerRunsPolicy - 让调用线程自己执行，起到限流作用
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());

        return executor;
    }

    /**
     * 轻量级任务线程池（用于缓存预热、日志记录等）
     */
    @Bean("lightTaskExecutor")
    public Executor lightTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(4);
        executor.setMaxPoolSize(8);
        executor.setQueueCapacity(200);
        executor.setThreadNamePrefix("light-task-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.DiscardPolicy());
        executor.initialize();
        return executor;
    }
}