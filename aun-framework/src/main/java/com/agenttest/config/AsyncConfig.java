package com.agenttest.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

/**
 * 异步任务配置 — 为评测引擎提供线程池。
 * <p>
 * 核心线程 4，最大线程 8，队列容量 100。
 * 评测任务逐个执行（每个 Agent×题目 组合提交一个异步任务），
 * 取消时通过 Future.cancel(true) 中断。
 */
@Configuration
@EnableAsync
public class AsyncConfig {

    @Bean("evaluationExecutor")
    public Executor evaluationExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(4);
        executor.setMaxPoolSize(8);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("eval-");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.initialize();
        return executor;
    }
}
