package com.jing.salesrankingbackend;

import com.jing.salesrankingbackend.support.IntegrationTest;
import tools.jackson.databind.ObjectMapper;
import com.jing.salesrankingbackend.constant.RankRedisConstants;
import com.jing.salesrankingbackend.dto.MerchantRankInfoDTO;
import com.jing.salesrankingbackend.dto.MerchantRankQueryDTO;
import com.jing.salesrankingbackend.dto.RankCachePayload;
import com.jing.salesrankingbackend.service.MerchantRankService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ActiveProfiles("test")
@IntegrationTest
class MerchantRankServiceTest {

    private static final String TEST_DATE = "2026-06-17";

    @Autowired
    private MerchantRankService merchantRankService;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        merchantRankService.syncYesterdayRankToRedis();
    }

    @Test
    void shouldSyncAndQueryRankFromRedis() {
        MerchantRankQueryDTO query = new MerchantRankQueryDTO();
        query.setCityId("000000");
        query.setType(0);
        query.setCategory(0);

        List<MerchantRankInfoDTO> result = merchantRankService.queryRankFromRedis(query);

        assertFalse(result.isEmpty());
        assertEquals(100, result.size());
        assertEquals(100001L, result.getFirst().getMerchantId());
        assertEquals(1, result.getFirst().getSort());
    }

    @Test
    void shouldStoreSingleJsonCacheKey() throws Exception {
        String yesterday = LocalDate.now().minusDays(1).format(DateTimeFormatter.ISO_LOCAL_DATE);
        if (!TEST_DATE.equals(yesterday)) {
            return;
        }

        String cacheKey = RankRedisConstants.buildCacheKey(yesterday, 0, "000000", 0);
        String cacheValue = stringRedisTemplate.opsForValue().get(cacheKey);

        assertNotNull(cacheValue);
        RankCachePayload payload = objectMapper.readValue(cacheValue, RankCachePayload.class);
        assertEquals(100, payload.getItems().size());

        Long ttl = stringRedisTemplate.getExpire(cacheKey, TimeUnit.DAYS);
        assertTrue(ttl != null && ttl > 0 && ttl <= RankRedisConstants.PHYSICAL_EXPIRE_DAYS);
    }

    @Test
    void shouldFallbackToPreviousDayWhenLatestCacheMissing() throws Exception {
        String yesterday = LocalDate.now().minusDays(1).format(DateTimeFormatter.ISO_LOCAL_DATE);
        String previousDay = LocalDate.now().minusDays(2).format(DateTimeFormatter.ISO_LOCAL_DATE);
        if (!TEST_DATE.equals(yesterday)) {
            return;
        }

        stringRedisTemplate.delete(RankRedisConstants.buildCacheKey(yesterday, 0, "000000", 0));

        RankCachePayload payload = new RankCachePayload();
        RankCachePayload.RankCacheItem item = new RankCachePayload.RankCacheItem();
        item.setMerchantId(888888L);
        item.setSort(1);
        item.setSaleNumMonth(200);
        item.setSaleNumDay(20);
        payload.setItems(List.of(item));
        stringRedisTemplate.opsForValue().set(
                RankRedisConstants.buildCacheKey(previousDay, 0, "000000", 0),
                objectMapper.writeValueAsString(payload)
        );

        MerchantRankQueryDTO query = new MerchantRankQueryDTO();
        query.setCityId("000000");
        query.setType(0);
        query.setCategory(0);

        List<MerchantRankInfoDTO> result = merchantRankService.queryRankFromRedis(query);

        assertFalse(result.isEmpty());
        assertEquals(previousDay, result.getFirst().getDate());
        assertEquals(888888L, result.getFirst().getMerchantId());
    }
}
