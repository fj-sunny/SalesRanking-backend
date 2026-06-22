package com.jing.salesrankingbackend;

import com.jing.salesrankingbackend.controller.MerchantRankController;
import com.jing.salesrankingbackend.dto.MerchantRankInfoDTO;
import com.jing.salesrankingbackend.dto.MerchantRankQueryDTO;
import com.jing.salesrankingbackend.service.MerchantRankService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class MerchantRankControllerTest {

    private MockMvc mockMvc;

    @Mock
    private MerchantRankService merchantRankService;

    @InjectMocks
    private MerchantRankController merchantRankController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(merchantRankController).build();
    }

    @Test
    void shouldQueryRankSuccessfully() throws Exception {
        MerchantRankInfoDTO first = new MerchantRankInfoDTO();
        first.setMerchantId(200001L);
        first.setSort(1);
        first.setSaleNumMonth(14948);
        first.setSaleNumDay(505);
        first.setDate("2026-06-21");

        when(merchantRankService.queryRankFromRedis(any(MerchantRankQueryDTO.class)))
                .thenReturn(List.of(first));

        mockMvc.perform(post("/api/rank/query")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "cityId": "000000",
                                  "type": 0,
                                  "category": 0
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("success"))
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].merchantId").value(200001))
                .andExpect(jsonPath("$.data[0].sort").value(1))
                .andExpect(jsonPath("$.data[0].date").value("2026-06-21"));
    }

    @Test
    void shouldReturnEmptyListWhenNoRankData() throws Exception {
        when(merchantRankService.queryRankFromRedis(any(MerchantRankQueryDTO.class)))
                .thenReturn(List.of());

        mockMvc.perform(post("/api/rank/query")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "cityId": "518000",
                                  "type": 1,
                                  "category": 1
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.length()").value(0));
    }

    @Test
    void shouldReturnFailWhenParameterInvalid() throws Exception {
        when(merchantRankService.queryRankFromRedis(any(MerchantRankQueryDTO.class)))
                .thenThrow(new IllegalArgumentException("cityId 不能为空"));

        mockMvc.perform(post("/api/rank/query")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "cityId": "",
                                  "type": 0,
                                  "category": 0
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500))
                .andExpect(jsonPath("$.message").value("cityId 不能为空"));
    }

    @Test
    void shouldAcceptDifferentCityTypeAndCategory() throws Exception {
        MerchantRankInfoDTO item = new MerchantRankInfoDTO();
        item.setMerchantId(200501L);
        item.setSort(1);
        item.setSaleNumMonth(16280);
        item.setSaleNumDay(814);
        item.setDate("2026-06-21");

        when(merchantRankService.queryRankFromRedis(any(MerchantRankQueryDTO.class)))
                .thenReturn(List.of(item));

        mockMvc.perform(post("/api/rank/query")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "cityId": "518000",
                                  "type": 1,
                                  "category": 2
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data[0].merchantId").value(200501))
                .andExpect(jsonPath("$.data[0].saleNumDay").value(814));
    }
}
