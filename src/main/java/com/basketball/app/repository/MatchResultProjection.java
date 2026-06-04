package com.basketball.app.repository;

import java.time.LocalDate;

/**
 * Match result fields for season summary.
 */
public interface MatchResultProjection {
    LocalDate getDate();
    Boolean getMatchWon();
    Long getCategoryId();
}
