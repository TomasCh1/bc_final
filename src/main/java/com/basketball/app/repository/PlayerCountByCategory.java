package com.basketball.app.repository;

/**
 * Projection for native query: player count per category.
 */
public interface PlayerCountByCategory {
    Long getCategoryId();
    Long getPlayerCount();
}
