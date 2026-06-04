package com.basketball.app.dto;

import com.basketball.app.model.Event;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalTime;

@Data
public class EventCreateRequest {
    @NotBlank(message = "Event name is required")
    private String name;

    @NotNull(message = "Event type is required")
    private Event.EventType type;

    @NotNull(message = "Event date is required")
    private LocalDate date;

    private LocalTime time;

    /** End time for training (optional). Only used for TRAINING. */
    private LocalTime endTime;

    private String location;

    @NotNull(message = "Event category is required")
    private Long categoryId;

    private String opponent;

    private String notes;

    /** True if match is at home (location in Trnava), false if away. Auto-determined from location if not provided. */
    private Boolean isHomeMatch;

    /** True if match was won, false if lost. Only applicable for MATCH type. */
    private Boolean matchWon;

    /** Our team's score (MATCH only). */
    private Integer ourScore;
    /** Opponent's score (MATCH only). */
    private Integer opponentScore;

    private Boolean isRecurring = false;
    
    private RecurrencePattern recurrencePattern;
    
    private Integer recurrenceInterval = 1;
    
    private LocalDate recurrenceEndDate;
    
    private Integer numberOfOccurrences;

    public enum RecurrencePattern {
        DAILY, WEEKLY, MONTHLY
    }
}

