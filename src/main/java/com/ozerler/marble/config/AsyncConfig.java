package com.ozerler.marble.config;

import com.ozerler.marble.common.Constants;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.security.task.DelegatingSecurityContextAsyncTaskExecutor;

@Configuration
public class AsyncConfig {

    @Bean(name = Constants.SEARCH_EXECUTOR)
    public AsyncTaskExecutor searchTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(Constants.SEARCH_CORE_POOL_SIZE);
        executor.setMaxPoolSize(Constants.SEARCH_MAX_POOL_SIZE);
        executor.setQueueCapacity(Constants.SEARCH_QUEUE_CAPACITY);
        executor.setThreadNamePrefix(Constants.THREAD_PREFIX_GLOBAL_SEARCH);
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(Constants.SEARCH_SHUTDOWN_SECONDS);
        executor.initialize();
        return new DelegatingSecurityContextAsyncTaskExecutor(executor);
    }
}
