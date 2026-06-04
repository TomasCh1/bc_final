package com.basketball.app.controller;

import com.basketball.app.service.ChartService;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(value = ChartController.class, excludeAutoConfiguration = SecurityAutoConfiguration.class)
@Tag("api")
class ChartControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ChartService chartService;

    @MockBean
    private com.basketball.app.security.JwtTokenProvider jwtTokenProvider;

    @MockBean
    private com.basketball.app.security.CustomUserDetailsService userDetailsService;

    @Test
    void timeSeries_ReturnsData() throws Exception {
        when(chartService.getTimeSeriesData(any(), anyString(), any(), any(), any(), any()))
                .thenReturn(Map.of("count", 0, "dataPoints", java.util.List.of()));

        mockMvc.perform(get("/api/charts/time-series").param("metric", "PTS"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.count").value(0));
    }

    @Test
    void comparative_ServiceFailure_ReturnsBadRequest() throws Exception {
        when(chartService.getComparativeAnalysis(anyString(), anyString(), any(), any(), any(), any()))
                .thenThrow(new RuntimeException("invalid metric"));

        mockMvc.perform(get("/api/charts/comparative"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("invalid metric"));
    }

    @Test
    void categoryDistribution_ReturnsData() throws Exception {
        when(chartService.getCategoryDistribution(anyString(), any(), any(), any()))
                .thenReturn(Map.of("categories", List.of()));

        mockMvc.perform(get("/api/charts/category-distribution").param("metric", "PTS"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.categories").isArray());
    }

    @Test
    void categoryDistribution_ServiceFailure_ReturnsBadRequest() throws Exception {
        when(chartService.getCategoryDistribution(anyString(), any(), any(), any()))
                .thenThrow(new RuntimeException("bad data"));

        mockMvc.perform(get("/api/charts/category-distribution"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("bad data"));
    }

    @Test
    void aggregatedStatistics_ReturnsData() throws Exception {
        when(chartService.getAggregatedStatistics(any(), any(), any(), any()))
                .thenReturn(Map.of("avgIndex", 15.2, "games", 8));

        mockMvc.perform(get("/api/charts/aggregated").param("playerId", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.avgIndex").value(15.2))
                .andExpect(jsonPath("$.games").value(8));
    }

    @Test
    void timeSeries_ServiceFailure_ReturnsBadRequest() throws Exception {
        when(chartService.getTimeSeriesData(any(), anyString(), any(), any(), any(), any()))
                .thenThrow(new RuntimeException("no data"));

        mockMvc.perform(get("/api/charts/time-series"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("no data"));
    }
}
