package com.jing.salesrankingbackend.service;

import com.jing.salesrankingbackend.dto.MerchantRankInfoDTO;
import com.jing.salesrankingbackend.dto.MerchantRankQueryDTO;

import java.util.List;

public interface MerchantRankService {

    /** 同步前一天排行榜到 Redis */
    int syncYesterdayRankToRedis();

    /** 从 Redis 查询排行榜，最新榜单不存在时回退历史榜单 */
    List<MerchantRankInfoDTO> queryRankFromRedis(MerchantRankQueryDTO query);
}
