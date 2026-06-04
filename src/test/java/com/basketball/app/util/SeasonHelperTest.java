package com.basketball.app.util;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.time.Year;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Tag("unit")
class SeasonHelperTest {

    @Test
    void getSeasonForYear_ReturnsExpectedRange() {
        assertEquals("2025-2026", SeasonHelper.getSeasonForYear(2025));
    }

    @Test
    void getDefaultSeason_UsesCurrentYear() {
        int currentYear = Year.now().getValue();
        String season = SeasonHelper.getDefaultSeason();
        assertTrue(season.startsWith(currentYear + "-"));
        assertTrue(season.endsWith(String.valueOf(currentYear + 1)));
    }
}
