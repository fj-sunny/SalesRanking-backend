package com.jing.salesrankingbackend.config;

import com.jing.salesrankingbackend.service.MerchantRankService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * 应用启动时预加载前一天排行榜到 Redis，避免首次查询缓存未命中。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MerchantRankCacheInitializer implements ApplicationRunner {

    private final MerchantRankService merchantRankService;

    @Value("${rank.sync.preload-on-startup:true}")
    private boolean preloadOnStartup;

    @Override
    public void run(ApplicationArguments args) {
        if (!preloadOnStartup) {
            return;
        }
        log.info("应用启动，开始预加载前一天排行榜到 Redis");
        int syncedCount = merchantRankService.syncYesterdayRankToRedis();
        log.info("启动预加载完成，共写入 {} 组排行榜数据", syncedCount);
    }
}
