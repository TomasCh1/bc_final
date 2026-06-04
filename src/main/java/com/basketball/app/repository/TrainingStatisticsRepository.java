package com.basketball.app.repository;

import com.basketball.app.model.BehStats;
import com.basketball.app.model.PoslnovanieStats;
import com.basketball.app.model.StrelbaStats;
import com.basketball.app.model.TrainingStatistics;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface TrainingStatisticsRepository extends JpaRepository<TrainingStatistics, Long> {
    List<TrainingStatistics> findByUserId(Long userId);

    List<TrainingStatistics> findByUserIdOrderByCreatedAtDesc(Long userId);

    @Query("SELECT t FROM TrainingStatistics t WHERE TYPE(t) = :type")
    List<TrainingStatistics> findByType(@Param("type") Class<? extends TrainingStatistics> type);

    default List<TrainingStatistics> findByCategory(TrainingStatistics.TrainingCategory category) {
        if (category == null) return List.of();
        return findByType(switch (category) {
            case POSLNOVANIE -> PoslnovanieStats.class;
            case BEH -> BehStats.class;
            case STRELBA -> StrelbaStats.class;
        });
    }

    @Query("SELECT t FROM TrainingStatistics t WHERE t.userId = :userId AND TYPE(t) = :type")
    List<TrainingStatistics> findByUserIdAndType(@Param("userId") Long userId, @Param("type") Class<? extends TrainingStatistics> type);

    default List<TrainingStatistics> findByUserIdAndCategory(Long userId, TrainingStatistics.TrainingCategory category) {
        if (category == null) return findByUserId(userId);
        return findByUserIdAndType(userId, switch (category) {
            case POSLNOVANIE -> PoslnovanieStats.class;
            case BEH -> BehStats.class;
            case STRELBA -> StrelbaStats.class;
        });
    }
    
    @Query("SELECT t FROM TrainingStatistics t WHERE t.userId = :userId AND t.createdAt >= :startDate AND t.createdAt <= :endDate ORDER BY t.createdAt DESC")
    List<TrainingStatistics> findByUserIdAndDateRange(@Param("userId") Long userId, 
                                                       @Param("startDate") LocalDateTime startDate, 
                                                       @Param("endDate") LocalDateTime endDate);
    
    @Query("SELECT t FROM TrainingStatistics t WHERE t.userId = :userId AND LOWER(t.exerciseName) LIKE LOWER(CONCAT('%', :searchTerm, '%')) ORDER BY t.createdAt DESC")
    List<TrainingStatistics> findByUserIdAndExerciseNameContaining(@Param("userId") Long userId, 
                                                                    @Param("searchTerm") String searchTerm);
}
