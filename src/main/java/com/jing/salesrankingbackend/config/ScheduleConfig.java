package com.jing.salesrankingbackend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

import java.util.concurrent.ScheduledExecutorService;

@Configuration
public class ScheduleConfig {

    @Bean(destroyMethod = "shutdown")
    public ScheduledExecutorService rankSyncRetryExecutor() {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(2);
        scheduler.setThreadNamePrefix("rank-sync-retry-");
        scheduler.initialize();
        return scheduler.getScheduledExecutor();
    }
}
