package com.jing.salesrankingbackend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;
import com.jing.salesrankingbackend.constant.RankRedisConstants;
import com.jing.salesrankingbackend.dto.MerchantRankInfoDTO;
import com.jing.salesrankingbackend.dto.MerchantRankQueryDTO;
import com.jing.salesrankingbackend.dto.RankCachePayload;
import com.jing.salesrankingbackend.entity.MerchantRankInfo;
import com.jing.salesrankingbackend.mapper.MerchantRankInfoMapper;
import com.jing.salesrankingbackend.service.MerchantRankService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class MerchantRankServiceImpl implements MerchantRankService {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE;

    private final MerchantRankInfoMapper merchantRankInfoMapper;
    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;

    @Override
    public int syncYesterdayRankToRedis() {
        String yesterday = LocalDate.now().minusDays(1).format(DATE_FORMATTER);
        return syncRankByDate(yesterday);
    }

    /** 按指定日期从数据库全量同步到 Redis，仅供内部及启动预加载使用 */
    private int syncRankByDate(String date) {
        List<MerchantRankInfo> rankList = listRankFromDb(date, null, null, null);
        if (CollectionUtils.isEmpty(rankList)) {
            log.warn("日期 {} 无排行榜数据，跳过 Redis 同步", date);
            return 0;
        }

        Map<String, List<MerchantRankInfo>> grouped = rankList.stream()
                .collect(Collectors.groupingBy(
                        item -> item.getCityId() + ":" + item.getType() + ":" + item.getCategory(),
                        LinkedHashMap::new,
                        Collectors.toList()
                ));

        grouped.values().forEach(items -> writeCache(date, deduplicateByMerchantId(items)));
        log.info("日期 {} 排行榜已同步到 Redis，共 {} 组", date, grouped.size());
        return grouped.size();
    }

    @Override
    public List<MerchantRankInfoDTO> queryRankFromRedis(MerchantRankQueryDTO query) {
        validateQuery(query);

        // 优先查昨天，再按天回退，最多回溯物理过期天数
        LocalDate latestDate = LocalDate.now().minusDays(1);
        for (int dayOffset = 0; dayOffset < RankRedisConstants.PHYSICAL_EXPIRE_DAYS; dayOffset++) {
            String date = latestDate.minusDays(dayOffset).format(DATE_FORMATTER);
            RankCachePayload cache = readCache(date, query.getType(), query.getCityId(), query.getCategory());
            if (cache != null && !CollectionUtils.isEmpty(cache.getItems())) {
                if (dayOffset > 0) {
                    log.info("未命中最新榜单，回退返回历史榜单，date={}, cityId={}, type={}, category={}",
                            date, query.getCityId(), query.getType(), query.getCategory());
                }
                return convertToDto(date, cache.getItems());
            }
        }
        return List.of();
    }

    /** 从数据库查询排行榜，支持按城市、榜单类型、类目过滤 */
    private List<MerchantRankInfo> listRankFromDb(
            String date, String cityId, Integer type, Integer category) {
        LambdaQueryWrapper<MerchantRankInfo> wrapper = new LambdaQueryWrapper<MerchantRankInfo>()
                .eq(MerchantRankInfo::getDate, date)
                .orderByAsc(MerchantRankInfo::getSort);
        if (StringUtils.hasText(cityId)) {
            wrapper.eq(MerchantRankInfo::getCityId, cityId);
        }
        if (type != null) {
            wrapper.eq(MerchantRankInfo::getType, type);
        }
        if (category != null) {
            wrapper.eq(MerchantRankInfo::getCategory, category);
        }
        return merchantRankInfoMapper.selectList(wrapper);
    }

    /** 全量覆盖写入 Redis，重复执行不会产生叠加数据 */
    private void writeCache(String date, List<MerchantRankInfo> items) {
        MerchantRankInfo first = items.getFirst();
        String cacheKey = RankRedisConstants.buildCacheKey(
                date, first.getType(), first.getCityId(), first.getCategory());
        stringRedisTemplate.opsForValue().set(
                cacheKey,
                toJson(buildCachePayload(items)),
                RankRedisConstants.PHYSICAL_EXPIRE_DAYS,
                TimeUnit.DAYS
        );
    }

    /** 同一商家只保留排名最高的一条 */
    private List<MerchantRankInfo> deduplicateByMerchantId(List<MerchantRankInfo> items) {
        Map<Long, MerchantRankInfo> uniqueItems = new LinkedHashMap<>();
        for (MerchantRankInfo item : items) {
            MerchantRankInfo existing = uniqueItems.get(item.getMerchantId());
            if (existing == null || item.getSort() < existing.getSort()) {
                uniqueItems.put(item.getMerchantId(), item);
            }
        }
        return uniqueItems.values().stream()
                .sorted(Comparator.comparing(MerchantRankInfo::getSort))
                .toList();
    }

    private RankCachePayload buildCachePayload(List<MerchantRankInfo> items) {
        RankCachePayload payload = new RankCachePayload();
        payload.setItems(items.stream().map(this::toCacheItem).toList());
        return payload;
    }

    private RankCachePayload.RankCacheItem toCacheItem(MerchantRankInfo item) {
        RankCachePayload.RankCacheItem cacheItem = new RankCachePayload.RankCacheItem();
        cacheItem.setMerchantId(item.getMerchantId());
        cacheItem.setSort(item.getSort());
        cacheItem.setSaleNumMonth(item.getSaleNumMonth());
        cacheItem.setSaleNumDay(item.getSaleNumDay());
        return cacheItem;
    }

    private RankCachePayload readCache(String date, Integer type, String cityId, Integer category) {
        String cacheValue = stringRedisTemplate.opsForValue().get(
                RankRedisConstants.buildCacheKey(date, type, cityId, category));
        if (!StringUtils.hasText(cacheValue)) {
            return null;
        }
        try {
            return objectMapper.readValue(cacheValue, RankCachePayload.class);
        } catch (JacksonException ex) {
            log.warn("排行榜缓存反序列化失败，date={}, cityId={}, type={}, category={}",
                    date, cityId, type, category, ex);
            return null;
        }
    }

    private String toJson(RankCachePayload payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JacksonException ex) {
            throw new IllegalStateException("排行榜缓存序列化失败", ex);
        }
    }

    /** 按 sort 升序返回，保证榜单展示顺序正确 */
    private List<MerchantRankInfoDTO> convertToDto(String date, List<RankCachePayload.RankCacheItem> items) {
        return items.stream()
                .sorted(Comparator.comparing(RankCachePayload.RankCacheItem::getSort))
                .map(item -> {
                    MerchantRankInfoDTO dto = new MerchantRankInfoDTO();
                    dto.setDate(date);
                    dto.setMerchantId(item.getMerchantId());
                    dto.setSort(item.getSort());
                    dto.setSaleNumMonth(item.getSaleNumMonth());
                    dto.setSaleNumDay(item.getSaleNumDay());
                    return dto;
                })
                .toList();
    }

    private void validateQuery(MerchantRankQueryDTO query) {
        if (query == null) {
            throw new IllegalArgumentException("请求参数不能为空");
        }
        if (!StringUtils.hasText(query.getCityId())) {
            throw new IllegalArgumentException("cityId 不能为空");
        }
        if (query.getType() == null) {
            throw new IllegalArgumentException("type 不能为空");
        }
        if (query.getCategory() == null) {
            throw new IllegalArgumentException("category 不能为空");
        }
    }
}
