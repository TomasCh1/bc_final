package com.basketball.app.util;

import java.time.Year;

public class SeasonHelper {
    
    /**
     * Get default season based on current year
     * If created in 2025, returns "2025-2026"
     */
    public static String getDefaultSeason() {
        int currentYear = Year.now().getValue();
        return currentYear + "-" + (currentYear + 1);
    }
    
    /**
     * Get season for a specific year
     */
    public static String getSeasonForYear(int year) {
        return year + "-" + (year + 1);
    }
}

