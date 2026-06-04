package com.basketball.app.dto;

import lombok.Data;

@Data
public class CategoryUpdateRequest {
    private String name;
    private String season;
    private Long coachId;
    private java.util.List<Long> coachIds;
}

