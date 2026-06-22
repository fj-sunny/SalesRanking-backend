package com.jing.salesrankingbackend;

import com.jing.salesrankingbackend.support.IntegrationTest;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.jing.salesrankingbackend.dto.MerchantRankQueryDTO;
import com.jing.salesrankingbackend.entity.MerchantRankInfo;
import com.jing.salesrankingbackend.mapper.MerchantRankInfoMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.assertFalse;

@SpringBootTest
@ActiveProfiles("test")
@IntegrationTest
class MerchantRankInfoMapperTest {

    @Autowired
    private MerchantRankInfoMapper merchantRankInfoMapper;

    @Test
    void shouldQueryByCityTypeAndCategory() {
        MerchantRankQueryDTO query = new MerchantRankQueryDTO();
        query.setCityId("000000");
        query.setType(0);
        query.setCategory(0);

        LambdaQueryWrapper<MerchantRankInfo> wrapper = new LambdaQueryWrapper<MerchantRankInfo>()
                .eq(MerchantRankInfo::getCityId, query.getCityId())
                .eq(MerchantRankInfo::getType, query.getType())
                .eq(MerchantRankInfo::getCategory, query.getCategory())
                .eq(MerchantRankInfo::getDate, "2026-06-17")
                .orderByAsc(MerchantRankInfo::getSort);

        assertFalse(merchantRankInfoMapper.selectList(wrapper).isEmpty());
    }
}
