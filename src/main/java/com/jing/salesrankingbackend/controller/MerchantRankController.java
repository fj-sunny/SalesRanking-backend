package com.jing.salesrankingbackend.controller;

import com.jing.salesrankingbackend.dto.ApiResponse;
import com.jing.salesrankingbackend.dto.MerchantRankInfoDTO;
import com.jing.salesrankingbackend.dto.MerchantRankQueryDTO;
import com.jing.salesrankingbackend.service.MerchantRankService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/rank")
@RequiredArgsConstructor
public class MerchantRankController {

    private final MerchantRankService merchantRankService;

    @PostMapping("/query")
    public ApiResponse<List<MerchantRankInfoDTO>> queryRank(@RequestBody MerchantRankQueryDTO query) {
        try {
            List<MerchantRankInfoDTO> data = merchantRankService.queryRankFromRedis(query);
            return ApiResponse.success(data);
        } catch (IllegalArgumentException ex) {
            return ApiResponse.fail(ex.getMessage());
        }
    }
}
