package com.basketball.app.dto;

import com.basketball.app.model.TrainingStatistics;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class TrainingStatisticsCreateRequest {
    @NotNull(message = "User ID is required")
    private Long userId;

    @NotBlank(message = "Exercise name is required")
    @Size(max = 64, message = "Exercise name must not exceed 64 characters")
    private String exerciseName;

    @NotNull(message = "Category is required")
    private TrainingStatistics.TrainingCategory category;

    // For POSLNOVANIE
    private BigDecimal weight;
    private Integer repetitions;

    // For BEH
    private String time;
    private BigDecimal distance;

    // For STRELBA
    private Integer shotsAttempted;
    private Integer shotsMade;
    private String courtSpot;

    // Available for all categories
    private String note;

    // Date when the workout actually happened (defaults to today if not provided)
    private LocalDate workoutDate;
}
