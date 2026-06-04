package com.basketball.app.dto;

import com.basketball.app.model.EventLiveAction;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class EventLiveActionCreateRequest {

    @NotNull(message = "Period is required")
    @Min(value = 1, message = "Period must be positive")
    private Integer period;

    @NotNull(message = "Clock seconds are required")
    @Min(value = 0, message = "Clock seconds must be >= 0")
    private Integer clockSeconds;

    @NotNull(message = "Team side is required")
    private EventLiveAction.TeamSide teamSide;

    @NotNull(message = "Action type is required")
    private EventLiveAction.ActionType actionType;

    private Long ourPlayerId;

    @Min(value = 1, message = "Opponent jersey number must be positive")
    private Integer opponentJerseyNumber;

    @Min(value = 0, message = "Points must be >= 0")
    private Integer points;

    private List<Long> onCourtPlayerIds;
}
