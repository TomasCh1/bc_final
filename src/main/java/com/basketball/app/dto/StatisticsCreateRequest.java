package com.basketball.app.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.Map;

/** Request for creating match statistics only. */
@Data
public class StatisticsCreateRequest {
    @NotNull(message = "Event ID is required")
    private Long eventId;

    @NotNull(message = "Player ID is required")
    private Long playerId;

    @NotNull(message = "Stat values are required")
    private Map<String, Object> values;

    @NotBlank(message = "Opponent is required for match statistics")
    private String opponent;

    /** Optional: our team's score (match only). When provided, updates the event's score. */
    private Integer ourScore;
    /** Optional: opponent's score (match only). When provided, updates the event's score. */
    private Integer opponentScore;
}


