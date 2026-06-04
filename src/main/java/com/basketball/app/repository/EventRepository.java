package com.basketball.app.repository;

import com.basketball.app.model.Event;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface EventRepository extends JpaRepository<Event, Long> {
    List<Event> findByCategoryId(Long categoryId);
    
    List<Event> findByType(Event.EventType type);
    
    List<Event> findByDate(LocalDate date);
    
    List<Event> findByDateBetween(LocalDate startDate, LocalDate endDate);
    
    List<Event> findByCategoryIdAndType(Long categoryId, Event.EventType type);
    
    List<Event> findByCategoryIdAndDateBetween(Long categoryId, LocalDate startDate, LocalDate endDate);

    /** Lightweight: only date, matchWon, categoryId for matches in range (for season summary). */
    @Query("SELECT e.date as date, e.matchWon as matchWon, e.categoryId as categoryId FROM Event e WHERE e.type = :matchType AND e.date BETWEEN :startDate AND :endDate")
    List<MatchResultProjection> findMatchResultsBetween(@Param("matchType") Event.EventType matchType, @Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);
    
    @Query("SELECT e FROM Event e WHERE " +
           "(:categoryId IS NULL OR e.categoryId = :categoryId) AND " +
           "(:type IS NULL OR e.type = :type) AND " +
           "(:startDate IS NULL OR e.date >= :startDate) AND " +
           "(:endDate IS NULL OR e.date <= :endDate) AND " +
           "(:search IS NULL OR :search = '' OR " +
           "LOWER(e.name) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "(e.location IS NOT NULL AND LOWER(e.location) LIKE LOWER(CONCAT('%', :search, '%'))) OR " +
           "(e.opponent IS NOT NULL AND LOWER(e.opponent) LIKE LOWER(CONCAT('%', :search, '%'))))")
    List<Event> findWithFilters(@Param("categoryId") Long categoryId,
                                @Param("type") Event.EventType type,
                                @Param("startDate") LocalDate startDate,
                                @Param("endDate") LocalDate endDate,
                                @Param("search") String search);

    /** Closest match on or after the given date; optional category filter. */
    Optional<Event> findFirstByTypeAndDateGreaterThanEqualOrderByDateAscTimeAsc(
            Event.EventType type, LocalDate date);

    Optional<Event> findFirstByTypeAndCategoryIdAndDateGreaterThanEqualOrderByDateAscTimeAsc(
            Event.EventType type, Long categoryId, LocalDate date);

    /** Event IDs that are MATCH and have at least one GAME statistic, ordered by date desc. */
    @Query(value = "SELECT e.id FROM Event e WHERE e.type = com.basketball.app.model.Event$EventType.MATCH AND e.id IN (SELECT s.eventId FROM Statistics s WHERE s.statType = com.basketball.app.model.Statistics$StatType.GAME) AND (:categoryId IS NULL OR e.categoryId = :categoryId) ORDER BY e.date DESC, e.time DESC",
           countQuery = "SELECT COUNT(e.id) FROM Event e WHERE e.type = com.basketball.app.model.Event$EventType.MATCH AND e.id IN (SELECT s.eventId FROM Statistics s WHERE s.statType = com.basketball.app.model.Statistics$StatType.GAME) AND (:categoryId IS NULL OR e.categoryId = :categoryId)")
    Page<Long> findRecentMatchEventIdsWithGameStats(@Param("categoryId") Long categoryId, Pageable pageable);
}

