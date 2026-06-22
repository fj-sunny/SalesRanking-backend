package com.jing.salesrankingbackend.schedule;

import com.jing.salesrankingbackend.service.MerchantRankService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@RequiredArgsConstructor
public class MerchantRankSyncScheduler {

    private static final long RETRY_DELAY_MINUTES = 5;
    private static final int MAX_RETRY_ATTEMPTS = 24;

    private final MerchantRankService merchantRankService;
    private final ScheduledExecutorService rankSyncRetryExecutor;

    /** 每天中午12点，同步前一天排行榜数据到 Redis */
    @Scheduled(cron = "0 0 12 * * ?")
    public void syncYesterdayRankToRedis() {
        executeSyncWithRetry(0);
    }

    private void executeSyncWithRetry(int attempt) {
        log.info("开始执行排行榜 Redis 同步任务，第 {} 次", attempt + 1);
        int syncedCount = merchantRankService.syncYesterdayRankToRedis();
        if (syncedCount > 0) {
            log.info("排行榜 Redis 同步成功，共写入 {} 组数据", syncedCount);
            return;
        }

        if (attempt + 1 >= MAX_RETRY_ATTEMPTS) {
            log.warn("排行榜 Redis 同步失败，已达最大重试次数 {}", MAX_RETRY_ATTEMPTS);
            return;
        }

        log.warn("排行榜 Redis 未写入新数据，{} 分钟后重试", RETRY_DELAY_MINUTES);
        rankSyncRetryExecutor.schedule(
                () -> executeSyncWithRetry(attempt + 1),
                RETRY_DELAY_MINUTES,
                TimeUnit.MINUTES
        );
    }
}
