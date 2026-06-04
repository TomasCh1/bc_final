package com.basketball.app.controller;

import com.basketball.app.model.Statistics;
import com.basketball.app.service.ChartService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/charts")
public class ChartController {

    private final ChartService chartService;

    public ChartController(ChartService chartService) {
        this.chartService = chartService;
    }

    /**
     * Get time-series data for performance metrics over time
     *
     * @param playerId Optional player ID to filter by specific player
     * @param categoryId Optional category ID to filter by category
     * @param metric Metric name (e.g., "INDEX", "PTS", "AST", "OFF", "DEF", "STL", "BLK", "TO")
     * @param startDate Optional start date for filtering
     * @param endDate Optional end date for filtering
     * @param statType Optional stat type filter (only GAME/match is stored in statistics)
     * @return Time-series data points
     */
    @GetMapping("/time-series")
    public ResponseEntity<?> getTimeSeriesData(
            @RequestParam(required = false) Long playerId,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false, defaultValue = "INDEX") String metric,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) Statistics.StatType statType) {
        try {
            Map<String, Object> data = chartService.getTimeSeriesData(playerId, metric, startDate, endDate, statType, categoryId);
            return ResponseEntity.ok(data);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Get category distribution data
     * 
     * @param metric Metric name to analyze
     * @param startDate Optional start date for filtering
     * @param endDate Optional end date for filtering
     * @param statType Optional stat type filter (only GAME/match is stored in statistics)
     * @return Category distribution data
     */
    @GetMapping("/category-distribution")
    public ResponseEntity<?> getCategoryDistribution(
            @RequestParam(required = false, defaultValue = "INDEX") String metric,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) Statistics.StatType statType) {
        try {
            Map<String, Object> data = chartService.getCategoryDistribution(metric, startDate, endDate, statType);
            return ResponseEntity.ok(data);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Get comparative analysis data
     *
     * @param metric Metric name to compare
     * @param comparisonType Type of comparison: "players" or "gameVsTraining"
     * @param playerIds Optional list of player IDs for player comparison
     * @param categoryId Optional category ID to filter by category
     * @param startDate Optional start date for filtering
     * @param endDate Optional end date for filtering
     * @return Comparative analysis data
     */
    @GetMapping("/comparative")
    public ResponseEntity<?> getComparativeAnalysis(
            @RequestParam(required = false, defaultValue = "INDEX") String metric,
            @RequestParam(required = false, defaultValue = "players") String comparisonType,
            @RequestParam(required = false) List<Long> playerIds,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        try {
            Map<String, Object> data = chartService.getComparativeAnalysis(
                    metric, comparisonType, playerIds, startDate, endDate, categoryId);
            return ResponseEntity.ok(data);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
        }
    }


    /**
     * Get aggregated statistics summary
     * 
     * @param playerId Optional player ID to filter by specific player
     * @param startDate Optional start date for filtering
     * @param endDate Optional end date for filtering
     * @param statType Optional stat type filter (only GAME/match is stored in statistics)
     * @return Aggregated statistics summary
     */
    @GetMapping("/aggregated")
    public ResponseEntity<?> getAggregatedStatistics(
            @RequestParam(required = false) Long playerId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) Statistics.StatType statType) {
        try {
            Map<String, Object> data = chartService.getAggregatedStatistics(playerId, startDate, endDate, statType);
            return ResponseEntity.ok(data);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
        }
    }
}
