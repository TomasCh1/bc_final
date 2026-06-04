package com.basketball.app.dto;

import lombok.Data;

@Data
public class EventNominationItemRequest {
    private Long playerId;
    private Integer jerseyNumber;
    private Boolean startingFive;
}
