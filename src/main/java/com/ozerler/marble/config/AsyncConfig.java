package com.ozerler.marble.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.security.task.DelegatingSecurityContextAsyncTaskExecutor;

@Configuration
public class AsyncConfig {

    public static final String SEARCH_EXECUTOR = "searchTaskExecutor";
    private static final int SEARCH_CORE_POOL_SIZE = 4;
    private static final int SEARCH_MAX_POOL_SIZE = 13;
    private static final int SEARCH_QUEUE_CAPACITY = 32;
    private static final int SEARCH_SHUTDOWN_SECONDS = 10;

    @Bean(name = SEARCH_EXECUTOR)
    public AsyncTaskExecutor searchTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(SEARCH_CORE_POOL_SIZE);
        executor.setMaxPoolSize(SEARCH_MAX_POOL_SIZE);
        executor.setQueueCapacity(SEARCH_QUEUE_CAPACITY);
        executor.setThreadNamePrefix("global-search-");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(SEARCH_SHUTDOWN_SECONDS);
        executor.initialize();
        return new DelegatingSecurityContextAsyncTaskExecutor(executor);
    }
}
