package com.basketball.app.controller;

import com.basketball.app.dto.TrainingStatisticsCreateRequest;
import com.basketball.app.dto.TrainingStatisticsUpdateRequest;
import com.basketball.app.model.BehStats;
import com.basketball.app.model.TrainingStatistics;
import com.basketball.app.service.TrainingStatisticsService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(value = TrainingStatisticsController.class, excludeAutoConfiguration = SecurityAutoConfiguration.class)
@Tag("api")
class TrainingStatisticsControllerTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private TrainingStatisticsService trainingStatisticsService;
    @MockBean
    private com.basketball.app.security.JwtTokenProvider jwtTokenProvider;
    @MockBean
    private com.basketball.app.security.CustomUserDetailsService userDetailsService;

    @Test
    void getTrainingStatisticsByUser_ReturnsList() throws Exception {
        BehStats stat = new BehStats();
        stat.setId(1L);
        stat.setUserId(3L);
        stat.setTime("00:22");
        stat.setDistance(new BigDecimal("120.0"));
        when(trainingStatisticsService.findByUserId(3L)).thenReturn(List.of(stat));

        mockMvc.perform(get("/api/training-statistics/user/3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1));
    }

    @Test
    void createTrainingStatistics_ServiceFailure_ReturnsBadRequest() throws Exception {
        when(trainingStatisticsService.createTrainingStatistics(any())).thenThrow(new RuntimeException("validation failed"));

        TrainingStatisticsCreateRequest request = new TrainingStatisticsCreateRequest();
        request.setUserId(1L);
        request.setExerciseName("Sprint");
        request.setCategory(TrainingStatistics.TrainingCategory.BEH);
        request.setTime("invalid");
        request.setDistance(new BigDecimal("10"));

        mockMvc.perform(post("/api/training-statistics")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("validation failed"));
    }

    @Test
    void createTrainingStatistics_Success_ReturnsCreated() throws Exception {
        BehStats created = new BehStats();
        created.setId(5L);
        created.setUserId(1L);
        created.setTime("00:30");
        when(trainingStatisticsService.createTrainingStatistics(any())).thenReturn(created);

        TrainingStatisticsCreateRequest request = new TrainingStatisticsCreateRequest();
        request.setUserId(1L);
        request.setExerciseName("Sprint");
        request.setCategory(TrainingStatistics.TrainingCategory.BEH);
        request.setTime("00:30");
        request.setDistance(new BigDecimal("50"));

        mockMvc.perform(post("/api/training-statistics")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(5));
    }

    @Test
    void getTrainingStatisticsById_Success() throws Exception {
        BehStats stat = new BehStats();
        stat.setId(1L);
        when(trainingStatisticsService.findById(1L)).thenReturn(stat);

        mockMvc.perform(get("/api/training-statistics/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    void getTrainingStatisticsById_NotFound() throws Exception {
        when(trainingStatisticsService.findById(99L)).thenThrow(new RuntimeException("not found"));

        mockMvc.perform(get("/api/training-statistics/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("not found"));
    }

    @Test
    void getLastTrainingStatistics_ReturnsLimitedList() throws Exception {
        BehStats stat = new BehStats();
        stat.setId(2L);
        when(trainingStatisticsService.findLastNByUserId(3L, 4)).thenReturn(List.of(stat));

        mockMvc.perform(get("/api/training-statistics/user/3/last").param("limit", "4"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(2));
    }

    @Test
    void getTrainingStatisticsWithFilters_ReturnsList() throws Exception {
        BehStats stat = new BehStats();
        stat.setId(3L);
        when(trainingStatisticsService.findWithFilters(eq(3L), any(), any(), any(), any()))
                .thenReturn(List.of(stat));

        mockMvc.perform(get("/api/training-statistics")
                        .param("userId", "3")
                        .param("category", "BEH"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(3));
    }

    @Test
    void updateTrainingStatistics_Success() throws Exception {
        BehStats updated = new BehStats();
        updated.setId(1L);
        updated.setTime("00:45");
        when(trainingStatisticsService.updateTrainingStatistics(eq(1L), any())).thenReturn(updated);

        TrainingStatisticsUpdateRequest request = new TrainingStatisticsUpdateRequest();
        request.setTime("00:45");

        mockMvc.perform(put("/api/training-statistics/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.time").value("00:45"));
    }

    @Test
    void deleteTrainingStatistics_Success() throws Exception {
        doNothing().when(trainingStatisticsService).deleteTrainingStatistics(1L);

        mockMvc.perform(delete("/api/training-statistics/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Training statistics deleted successfully"));

        verify(trainingStatisticsService).deleteTrainingStatistics(1L);
    }
}
