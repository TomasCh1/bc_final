package com.basketball.app.dto;

import lombok.Data;

import java.util.Map;

/** Request for updating match statistics only. */
@Data
public class StatisticsUpdateRequest {
    private Map<String, Object> values;
    private String opponent;
    /** Optional: our team's score (match only). When provided, updates the event's score. */
    private Integer ourScore;
    /** Optional: opponent's score (match only). When provided, updates the event's score. */
    private Integer opponentScore;
}


