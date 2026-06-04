package com.basketball.app.controller;

import com.basketball.app.dto.StatisticsCreateRequest;
import com.basketball.app.dto.StatisticsUpdateRequest;
import com.basketball.app.model.Statistics;
import com.basketball.app.service.MatchStatsTemplateExportService;
import com.basketball.app.service.StatisticsService;
import com.basketball.app.service.UserService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/statistics")
public class StatisticsController {

    private final StatisticsService statisticsService;
    private final MatchStatsTemplateExportService matchStatsTemplateExportService;
    private final UserService userService;

    public StatisticsController(
            StatisticsService statisticsService,
            MatchStatsTemplateExportService matchStatsTemplateExportService,
            UserService userService
    ) {
        this.statisticsService = statisticsService;
        this.matchStatsTemplateExportService = matchStatsTemplateExportService;
        this.userService = userService;
    }

    /**
     * Create statistics record
     */
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'TRAINER')")
    public ResponseEntity<?> createStatistics(@Valid @RequestBody StatisticsCreateRequest request) {
        try {
            Statistics statistics = statisticsService.createStatistics(request);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(statisticsService.toStatisticsMap(statistics));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Update statistics record
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'TRAINER')")
    public ResponseEntity<?> updateStatistics(@PathVariable Long id,
                                             @Valid @RequestBody StatisticsUpdateRequest request) {
        try {
            Statistics statistics = statisticsService.updateStatistics(id, request);
            return ResponseEntity.ok(statisticsService.toStatisticsMap(statistics));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Get statistics by ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<?> getStatisticsById(@PathVariable Long id) {
        try {
            Statistics statistics = statisticsService.findById(id)
                    .orElseThrow(() -> new RuntimeException("Statistics not found: " + id));
            return ResponseEntity.ok(statisticsService.toStatisticsMap(statistics));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Get all statistics (match only). statType filter is optional; only GAME is stored.
     */
    @GetMapping
    public ResponseEntity<?> getAllStatistics(
            @RequestParam(required = false) Long playerId,
            @RequestParam(required = false) Long eventId,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) Statistics.StatType statType, // only GAME applies
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) String opponent) {
        try {
            List<Map<String, Object>> statisticsMaps = statisticsService.findWithFiltersAsMaps(
                    playerId, eventId, categoryId, statType, startDate, endDate, opponent);

            return ResponseEntity.ok(statisticsMaps);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Get statistics for the most recent N games (lazy load). Returns statistics list plus totalGames and hasMore.
     */
    @GetMapping("/games/recent")
    public ResponseEntity<?> getRecentGameStatistics(
            @RequestParam(defaultValue = "10") int limit,
            @RequestParam(defaultValue = "0") int offset,
            @RequestParam(required = false) Long categoryId) {
        try {
            limit = Math.min(Math.max(1, limit), 100);
            offset = Math.max(0, offset);
            return ResponseEntity.ok(statisticsService.findRecentGameStatistics(categoryId, limit, offset));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Get statistics by event
     */
    @GetMapping("/event/{eventId}")
    public ResponseEntity<?> getStatisticsByEvent(@PathVariable Long eventId) {
        try {
            List<Statistics> statistics = statisticsService.findByEvent(eventId);
            List<Map<String, Object>> statisticsMaps = statistics.stream()
                    .map(statisticsService::toStatisticsMap)
                    .collect(java.util.stream.Collectors.toList());
            return ResponseEntity.ok(statisticsMaps);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Get statistics by player
     */
    @GetMapping("/player/{playerId}")
    public ResponseEntity<?> getStatisticsByPlayer(@PathVariable Long playerId) {
        try {
            List<Statistics> statistics = statisticsService.findByPlayer(playerId);
            List<Map<String, Object>> statisticsMaps = statistics.stream()
                    .map(statisticsService::toStatisticsMap)
                    .collect(java.util.stream.Collectors.toList());
            return ResponseEntity.ok(statisticsMaps);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Get statistics by stat type (only GAME/match is stored).
     */
    @GetMapping("/type/{statType}")
    public ResponseEntity<?> getStatisticsByType(@PathVariable Statistics.StatType statType) {
        try {
            List<Statistics> statistics = statisticsService.findByStatType(statType);
            List<Map<String, Object>> statisticsMaps = statistics.stream()
                    .map(statisticsService::toStatisticsMap)
                    .collect(java.util.stream.Collectors.toList());
            return ResponseEntity.ok(statisticsMaps);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Delete statistics
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> deleteStatistics(@PathVariable Long id) {
        try {
            statisticsService.deleteStatistics(id);
            return ResponseEntity.ok(Map.of("message", "Statistics deleted successfully"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Calculate INDEX from stat values
     */
    @PostMapping("/calculate-index")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> calculateIndex(@RequestBody Map<String, Object> values) {
        try {
            Double index = statisticsService.calculateIndex(values);
            return ResponseEntity.ok(Map.of("INDEX", index));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Export one match into the pre-formatted XLSX template.
     */
    @GetMapping("/export/match-template")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> exportMatchTemplate(@RequestParam Long eventId) {
        try {
            String preparedBy = com.basketball.app.util.UserNames.toSurnameAndGivenName(userService.getCurrentUser());
            byte[] excelData = matchStatsTemplateExportService.exportMatchTemplate(eventId, preparedBy);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.parseMediaType(
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
            headers.setContentDispositionFormData(
                    "attachment", "match_statistics_" + eventId + ".xlsx");

            return ResponseEntity.ok()
                    .headers(headers)
                    .body(excelData);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "Failed to export match template: " + e.getMessage()));
        }
    }
}


