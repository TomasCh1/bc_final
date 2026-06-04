package com.basketball.app.controller;

import com.basketball.app.dto.StatisticsCreateRequest;
import com.basketball.app.dto.StatisticsUpdateRequest;
import com.basketball.app.model.Statistics;
import com.basketball.app.model.User;
import com.basketball.app.service.MatchStatsTemplateExportService;
import com.basketball.app.service.StatisticsService;
import com.basketball.app.service.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(value = StatisticsController.class, excludeAutoConfiguration = SecurityAutoConfiguration.class)
@Tag("api")
class StatisticsControllerTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private StatisticsService statisticsService;
    @MockBean
    private MatchStatsTemplateExportService matchStatsTemplateExportService;
    @MockBean
    private UserService userService;
    @MockBean
    private com.basketball.app.security.JwtTokenProvider jwtTokenProvider;
    @MockBean
    private com.basketball.app.security.CustomUserDetailsService userDetailsService;

    private Statistics testStatistics;
    private Map<String, Object> statsMap;

    @BeforeEach
    void setUp() {
        testStatistics = new Statistics();
        testStatistics.setId(1L);
        testStatistics.setEventId(10L);
        testStatistics.setPlayerId(20L);
        testStatistics.setOpponent("Rivals");
        testStatistics.setStatType(Statistics.StatType.GAME);

        statsMap = new HashMap<>();
        statsMap.put("id", 1L);
        statsMap.put("eventId", 10L);
        statsMap.put("playerId", 20L);
        statsMap.put("opponent", "Rivals");
    }

    @Test
    void getAllStatistics_ReturnsList() throws Exception {
        when(statisticsService.findWithFiltersAsMaps(any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(List.of(Map.of("id", 1)));

        mockMvc.perform(get("/api/statistics"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1));
    }

    @Test
    void calculateIndex_ReturnsComputedValue() throws Exception {
        when(statisticsService.calculateIndex(any())).thenReturn(18.5);

        mockMvc.perform(post("/api/statistics/calculate-index")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"PTS\":20,\"TO\":2}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.INDEX").value(18.5));
    }

    @Test
    void createStatistics_Success_ReturnsCreated() throws Exception {
        when(statisticsService.createStatistics(any())).thenReturn(testStatistics);
        when(statisticsService.toStatisticsMap(testStatistics)).thenReturn(statsMap);

        StatisticsCreateRequest request = new StatisticsCreateRequest();
        request.setEventId(10L);
        request.setPlayerId(20L);
        request.setOpponent("Rivals");
        request.setValues(Map.of("PTS", 12));

        mockMvc.perform(post("/api/statistics")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1));

        verify(statisticsService).createStatistics(any());
    }

    @Test
    void createStatistics_ServiceError_ReturnsBadRequest() throws Exception {
        when(statisticsService.createStatistics(any())).thenThrow(new RuntimeException("Event not found"));

        StatisticsCreateRequest request = new StatisticsCreateRequest();
        request.setEventId(99L);
        request.setPlayerId(20L);
        request.setOpponent("Rivals");
        request.setValues(Map.of("PTS", 5));

        mockMvc.perform(post("/api/statistics")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Event not found"));
    }

    @Test
    void updateStatistics_Success_ReturnsOk() throws Exception {
        when(statisticsService.updateStatistics(eq(1L), any())).thenReturn(testStatistics);
        when(statisticsService.toStatisticsMap(testStatistics)).thenReturn(statsMap);

        StatisticsUpdateRequest request = new StatisticsUpdateRequest();
        request.setOpponent("New Rivals");

        mockMvc.perform(put("/api/statistics/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.opponent").value("Rivals"));
    }

    @Test
    void getStatisticsById_Success_ReturnsMap() throws Exception {
        when(statisticsService.findById(1L)).thenReturn(Optional.of(testStatistics));
        when(statisticsService.toStatisticsMap(testStatistics)).thenReturn(statsMap);

        mockMvc.perform(get("/api/statistics/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    void getStatisticsById_NotFound_Returns404() throws Exception {
        when(statisticsService.findById(99L)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/statistics/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").exists());
    }

    @Test
    void getRecentGameStatistics_ReturnsPayload() throws Exception {
        Map<String, Object> payload = Map.of(
                "statistics", List.of(statsMap),
                "totalGames", 2L,
                "hasMore", false
        );
        when(statisticsService.findRecentGameStatistics(isNull(), eq(10), eq(0))).thenReturn(payload);

        mockMvc.perform(get("/api/statistics/games/recent").param("limit", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalGames").value(2))
                .andExpect(jsonPath("$.hasMore").value(false));
    }

    @Test
    void getStatisticsByEvent_ReturnsList() throws Exception {
        when(statisticsService.findByEvent(10L)).thenReturn(List.of(testStatistics));
        when(statisticsService.toStatisticsMap(testStatistics)).thenReturn(statsMap);

        mockMvc.perform(get("/api/statistics/event/10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].eventId").value(10));
    }

    @Test
    void getStatisticsByPlayer_ReturnsList() throws Exception {
        when(statisticsService.findByPlayer(20L)).thenReturn(List.of(testStatistics));
        when(statisticsService.toStatisticsMap(testStatistics)).thenReturn(statsMap);

        mockMvc.perform(get("/api/statistics/player/20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].playerId").value(20));
    }

    @Test
    void getStatisticsByType_ReturnsList() throws Exception {
        when(statisticsService.findByStatType(Statistics.StatType.GAME)).thenReturn(List.of(testStatistics));
        when(statisticsService.toStatisticsMap(testStatistics)).thenReturn(statsMap);

        mockMvc.perform(get("/api/statistics/type/GAME"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1));
    }

    @Test
    void deleteStatistics_Success_ReturnsMessage() throws Exception {
        doNothing().when(statisticsService).deleteStatistics(1L);

        mockMvc.perform(delete("/api/statistics/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Statistics deleted successfully"));

        verify(statisticsService).deleteStatistics(1L);
    }

    @Test
    void deleteStatistics_NotFound_ReturnsBadRequest() throws Exception {
        doThrow(new RuntimeException("Statistics not found: 99"))
                .when(statisticsService).deleteStatistics(99L);

        mockMvc.perform(delete("/api/statistics/99"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists());
    }

    @Test
    void exportMatchTemplate_Success_ReturnsExcelBytes() throws Exception {
        User user = new User();
        user.setName("Coach Admin");
        when(userService.getCurrentUser()).thenReturn(user);
        when(matchStatsTemplateExportService.exportMatchTemplate(5L, "Coach Admin"))
                .thenReturn(new byte[]{1, 2, 3});

        mockMvc.perform(get("/api/statistics/export/match-template").param("eventId", "5"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", containsString("match_statistics_5.xlsx")));
    }

    @Test
    void exportMatchTemplate_Failure_ReturnsBadRequest() throws Exception {
        User user = new User();
        user.setName("Coach");
        when(userService.getCurrentUser()).thenReturn(user);
        when(matchStatsTemplateExportService.exportMatchTemplate(anyLong(), anyString()))
                .thenThrow(new RuntimeException("template missing"));

        mockMvc.perform(get("/api/statistics/export/match-template").param("eventId", "5"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(containsString("Failed to export")));
    }
}
