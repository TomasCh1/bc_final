package com.basketball.app.repository;

import com.basketball.app.model.Statistics;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface StatisticsRepository extends JpaRepository<Statistics, Long> {
    List<Statistics> findByEventId(Long eventId);
    void deleteByEventId(Long eventId);
    
    List<Statistics> findByEventIdIn(List<Long> eventIds);
    
    List<Statistics> findByPlayerId(Long playerId);
    
    List<Statistics> findByStatType(Statistics.StatType statType);
    
    List<Statistics> findByPlayerIdAndStatType(Long playerId, Statistics.StatType statType);
    
    List<Statistics> findByEventIdAndPlayerId(Long eventId, Long playerId);
    
    @Query("SELECT s FROM Statistics s WHERE s.eventId IN " +
           "(SELECT e.id FROM Event e WHERE e.date BETWEEN :startDate AND :endDate)")
    List<Statistics> findByDateRange(@Param("startDate") LocalDate startDate, 
                                      @Param("endDate") LocalDate endDate);
    
    @Query("SELECT s FROM Statistics s WHERE s.playerId = :playerId AND " +
           "s.eventId IN (SELECT e.id FROM Event e WHERE e.date BETWEEN :startDate AND :endDate)")
    List<Statistics> findByPlayerIdAndDateRange(@Param("playerId") Long playerId,
                                                @Param("startDate") LocalDate startDate,
                                                @Param("endDate") LocalDate endDate);
    
    @Query("SELECT s FROM Statistics s WHERE s.eventId IN " +
           "(SELECT e.id FROM Event e WHERE e.categoryId = :categoryId)")
    List<Statistics> findByCategoryId(@Param("categoryId") Long categoryId);
    
    List<Statistics> findByOpponent(String opponent);
    
    List<Statistics> findByOpponentContainingIgnoreCase(String opponent);
}

