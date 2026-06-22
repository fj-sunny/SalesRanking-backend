package com.jing.salesrankingbackend.constant;

/**
 * 排行榜 Redis Key 规则：rank:sale:v1:{日期}:{榜单类型}:{城市}:{商品类型}
 */
public final class RankRedisConstants {

    private RankRedisConstants() {
    }

    public static final String RANK_KEY_PREFIX = "rank:sale:v1:";

    /** 物理过期时间：7天 */
    public static final long PHYSICAL_EXPIRE_DAYS = 7;

    public static String buildCacheKey(String date, Integer type, String cityId, Integer category) {
        return RANK_KEY_PREFIX + date + ":" + type + ":" + cityId + ":" + category;
    }
}
