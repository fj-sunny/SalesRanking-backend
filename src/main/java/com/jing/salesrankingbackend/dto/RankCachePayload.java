package com.jing.salesrankingbackend.dto;

import lombok.Data;

import java.util.List;

/**
 * 排行榜 Redis 缓存结构（单 Key JSON 存储）
 */
@Data
public class RankCachePayload {

    private List<RankCacheItem> items;

    @Data
    public static class RankCacheItem {

        private Long merchantId;
        private Integer sort;
        private Integer saleNumMonth;
        private Integer saleNumDay;
    }
}
