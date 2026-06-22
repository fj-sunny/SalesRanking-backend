package com.jing.salesrankingbackend.dto;

import lombok.Data;

/**
 * 排行榜查询入参
 */
@Data
public class MerchantRankQueryDTO {

    /** 城市id，全国为 000000 */
    private String cityId;

    /** 排行榜类型：0爆款榜 1飙升榜 */
    private Integer type;

    /** 类目类型：0全部类型 1美食 2休闲丽人 */
    private Integer category;
}
