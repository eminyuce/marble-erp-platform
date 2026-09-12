package com.ozerler.marble.config;

import com.ozerler.marble.common.Constants;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.security.task.DelegatingSecurityContextAsyncTaskExecutor;

@Configuration
public class AsyncConfig {

    public static final String SEARCH_EXECUTOR = Constants.SEARCH_EXECUTOR;
    private static final int SEARCH_CORE_POOL_SIZE = Constants.SEARCH_CORE_POOL_SIZE;
    private static final int SEARCH_MAX_POOL_SIZE = Constants.SEARCH_MAX_POOL_SIZE;
    private static final int SEARCH_QUEUE_CAPACITY = Constants.SEARCH_QUEUE_CAPACITY;
    private static final int SEARCH_SHUTDOWN_SECONDS = Constants.SEARCH_SHUTDOWN_SECONDS;

    @Bean(name = SEARCH_EXECUTOR)
    public AsyncTaskExecutor searchTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(SEARCH_CORE_POOL_SIZE);
        executor.setMaxPoolSize(SEARCH_MAX_POOL_SIZE);
        executor.setQueueCapacity(SEARCH_QUEUE_CAPACITY);
        executor.setThreadNamePrefix(Constants.THREAD_PREFIX_GLOBAL_SEARCH);
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(SEARCH_SHUTDOWN_SECONDS);
        executor.initialize();
        return new DelegatingSecurityContextAsyncTaskExecutor(executor);
    }
}
