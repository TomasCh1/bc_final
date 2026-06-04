package com.basketball.app.dto;

import lombok.Data;

import java.util.List;

@Data
public class EventOpponentsRequest {
    private List<Integer> jerseyNumbers;
}
