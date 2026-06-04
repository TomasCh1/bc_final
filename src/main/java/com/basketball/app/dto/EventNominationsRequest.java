package com.basketball.app.dto;

import lombok.Data;

import java.util.List;

@Data
public class EventNominationsRequest {
    private List<Long> playerIds;
    private List<EventNominationItemRequest> nominations;
}
