package com.basketball.app.repository;

import com.basketball.app.model.Attendance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface AttendanceRepository extends JpaRepository<Attendance, Long> {
    List<Attendance> findByEventId(Long eventId);
    void deleteByEventId(Long eventId);

    List<Attendance> findByEventIdIn(List<Long> eventIds);
    
    List<Attendance> findByPlayerId(Long playerId);
    
    Optional<Attendance> findByEventIdAndPlayerId(Long eventId, Long playerId);
    
    List<Attendance> findByPlayerIdAndStatus(Long playerId, Attendance.AttendanceStatus status);
    
    @Query("SELECT a FROM Attendance a " +
           "JOIN Event e ON e.id = a.eventId " +
           "WHERE e.date BETWEEN :startDate AND :endDate")
    List<Attendance> findByDateRange(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);
    
    @Query("SELECT a FROM Attendance a " +
           "JOIN Event e ON e.id = a.eventId " +
           "WHERE a.playerId = :playerId AND e.date BETWEEN :startDate AND :endDate")
    List<Attendance> findByPlayerIdAndDateRange(@Param("playerId") Long playerId,
                                                 @Param("startDate") LocalDate startDate,
                                                 @Param("endDate") LocalDate endDate);
    
    @Query("SELECT a FROM Attendance a " +
           "JOIN Event e ON e.id = a.eventId " +
           "WHERE e.categoryId = :categoryId")
    List<Attendance> findByCategoryId(@Param("categoryId") Long categoryId);
    
    @Query("SELECT COUNT(a) FROM Attendance a WHERE a.playerId = :playerId AND a.status = :status")
    Long countByPlayerIdAndStatus(@Param("playerId") Long playerId, @Param("status") Attendance.AttendanceStatus status);
    
    @Query("SELECT COUNT(a) FROM Attendance a " +
           "JOIN Event e ON e.id = a.eventId " +
           "WHERE a.playerId = :playerId AND e.date BETWEEN :startDate AND :endDate")
    Long countByPlayerIdAndDateRange(@Param("playerId") Long playerId,
                                     @Param("startDate") LocalDate startDate,
                                     @Param("endDate") LocalDate endDate);
    
    @Query("SELECT COUNT(a) FROM Attendance a " +
           "JOIN Event e ON e.id = a.eventId " +
           "WHERE a.playerId = :playerId AND a.status = :status AND e.date BETWEEN :startDate AND :endDate")
    Long countByPlayerIdAndStatusAndDateRange(@Param("playerId") Long playerId,
                                               @Param("status") Attendance.AttendanceStatus status,
                                               @Param("startDate") LocalDate startDate,
                                               @Param("endDate") LocalDate endDate);
}

