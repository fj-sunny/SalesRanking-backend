package com.jing.salesrankingbackend.dto;

import lombok.Data;

/**
 * 排行榜单条返回数据
 */
@Data
public class MerchantRankInfoDTO {

    /** 商家id */
    private Long merchantId;

    /** 排名 */
    private Integer sort;

    /** 月售数量 */
    private Integer saleNumMonth;

    /** 日售数量 */
    private Integer saleNumDay;

    /** 实际命中的榜单日期 */
    private String date;
}
