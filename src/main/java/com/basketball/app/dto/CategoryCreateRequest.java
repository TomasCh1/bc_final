package com.basketball.app.dto;

import com.basketball.app.util.SeasonHelper;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CategoryCreateRequest {
    @NotBlank(message = "Category name is required")
    private String name;

    private String season;

    private Long coachId;
    private java.util.List<Long> coachIds;
    
    /**
     * Get season, or default if not provided
     */
    public String getSeason() {
        if (season == null || season.trim().isEmpty()) {
            return SeasonHelper.getDefaultSeason();
        }
        return season;
    }
}

