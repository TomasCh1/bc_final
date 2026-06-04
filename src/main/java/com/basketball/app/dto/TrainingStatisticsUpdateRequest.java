package com.basketball.app.dto;

import com.basketball.app.model.TrainingStatistics;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class TrainingStatisticsUpdateRequest {
    @Size(max = 64, message = "Exercise name must not exceed 64 characters")
    private String exerciseName;

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

    // Date when the workout actually happened
    private LocalDate workoutDate;
}
