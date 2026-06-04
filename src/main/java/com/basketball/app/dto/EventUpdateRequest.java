package com.basketball.app.dto;

import com.basketball.app.model.Event;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalTime;

@Data
public class EventUpdateRequest {
    private String name;
    private Event.EventType type;
    private LocalDate date;
    private LocalTime time;
    /** End time for training (optional). Only used for TRAINING. */
    private LocalTime endTime;
    private String location;
    private Long categoryId;
    private String opponent;
    private String notes;
    /** True if match is at home (location in Trnava), false if away. Auto-determined from location if not provided. */
    private Boolean isHomeMatch;
    /** True if match was won, false if lost. Only applicable for MATCH type. */
    private Boolean matchWon;
    /** Our team's score (match only). */
    private Integer ourScore;
    /** Opponent's score (match only). */
    private Integer opponentScore;
    /** True if coach/admin cancelled this training. Only applicable for TRAINING type. */
    private Boolean cancelled;
}

