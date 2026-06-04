package com.basketball.app.controller;

import com.basketball.app.dto.TrainingStatisticsCreateRequest;
import com.basketball.app.dto.TrainingStatisticsUpdateRequest;
import com.basketball.app.model.TrainingStatistics;
import com.basketball.app.service.TrainingStatisticsService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/training-statistics")
public class TrainingStatisticsController {

    private final TrainingStatisticsService trainingStatisticsService;

    public TrainingStatisticsController(TrainingStatisticsService trainingStatisticsService) {
        this.trainingStatisticsService = trainingStatisticsService;
    }

    /**
     * Create training statistics record
     */
    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> createTrainingStatistics(@Valid @RequestBody TrainingStatisticsCreateRequest request) {
        try {
            TrainingStatistics statistics = trainingStatisticsService.createTrainingStatistics(request);
            return ResponseEntity.status(HttpStatus.CREATED).body(statistics);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Update training statistics record
     */
    @PutMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> updateTrainingStatistics(@PathVariable Long id,
                                                      @Valid @RequestBody TrainingStatisticsUpdateRequest request) {
        try {
            TrainingStatistics statistics = trainingStatisticsService.updateTrainingStatistics(id, request);
            return ResponseEntity.ok(statistics);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Get training statistics by ID
     */
    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> getTrainingStatisticsById(@PathVariable Long id) {
        try {
            TrainingStatistics statistics = trainingStatisticsService.findById(id);
            return ResponseEntity.ok(statistics);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Get all training statistics for a user
     */
    @GetMapping("/user/{userId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> getTrainingStatisticsByUser(@PathVariable Long userId) {
        try {
            List<TrainingStatistics> statistics = trainingStatisticsService.findByUserId(userId);
            return ResponseEntity.ok(statistics);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Get last N training statistics for a user
     */
    @GetMapping("/user/{userId}/last")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> getLastTrainingStatistics(@PathVariable Long userId,
                                                        @RequestParam(defaultValue = "4") int limit) {
        try {
            List<TrainingStatistics> statistics = trainingStatisticsService.findLastNByUserId(userId, limit);
            return ResponseEntity.ok(statistics);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Get training statistics with filters.
     */
    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> getTrainingStatisticsWithFilters(
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) TrainingStatistics.TrainingCategory category,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) String search) {
        try {
            List<TrainingStatistics> statistics = trainingStatisticsService.findWithFilters(
                    userId, category, startDate, endDate, search);

            return ResponseEntity.ok(statistics);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Delete training statistics
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> deleteTrainingStatistics(@PathVariable Long id) {
        try {
            trainingStatisticsService.deleteTrainingStatistics(id);
            return ResponseEntity.ok(Map.of("message", "Training statistics deleted successfully"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
        }
    }
}
