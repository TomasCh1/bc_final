package com.basketball.app.repository;

import com.basketball.app.model.BehStats;
import com.basketball.app.model.PoslnovanieStats;
import com.basketball.app.model.TrainingStatistics;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DataJpaTest(showSql = false)
@ActiveProfiles("test")
@Tag("integration")
class TrainingStatisticsRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;
    @Autowired
    private TrainingStatisticsRepository repository;

    @Test
    void supportsCategoryAndSearchQueries() {
        PoslnovanieStats pos = new PoslnovanieStats();
        pos.setUserId(1L);
        pos.setExerciseName("Bench press");
        pos.setWeight(new BigDecimal("50"));
        pos.setRepetitions(8);
        pos.setWorkoutDate(LocalDate.of(2026, 1, 10));
        pos.setCreatedAt(LocalDateTime.of(2026, 1, 10, 12, 0));

        BehStats run = new BehStats();
        run.setUserId(1L);
        run.setExerciseName("Sprint drill");
        run.setDistance(new BigDecimal("200"));
        run.setTime("00:30");
        run.setWorkoutDate(LocalDate.of(2026, 1, 11));
        run.setCreatedAt(LocalDateTime.of(2026, 1, 11, 12, 0));

        entityManager.persistAndFlush(pos);
        entityManager.persistAndFlush(run);

        assertEquals(2, repository.findByUserId(1L).size());
        assertEquals(1, repository.findByCategory(TrainingStatistics.TrainingCategory.POSLNOVANIE).size());
        assertEquals(1, repository.findByUserIdAndExerciseNameContaining(1L, "sprint").size());
    }
}
