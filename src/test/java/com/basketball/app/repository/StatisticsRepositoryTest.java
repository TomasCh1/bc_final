package com.basketball.app.repository;

import com.basketball.app.model.Event;
import com.basketball.app.model.Statistics;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DataJpaTest(showSql = false)
@ActiveProfiles("test")
@Tag("integration")
class StatisticsRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;
    @Autowired
    private StatisticsRepository repository;

    @Test
    void filtersByPlayerCategoryAndOpponent() {
        Event event = new Event();
        event.setName("Match");
        event.setType(Event.EventType.MATCH);
        event.setDate(LocalDate.of(2026, 2, 1));
        event.setCategoryId(9L);
        event.setOpponent("Rivals");
        entityManager.persistAndFlush(event);

        Statistics stat = new Statistics();
        stat.setEventId(event.getId());
        stat.setPlayerId(3L);
        stat.setStatType(Statistics.StatType.GAME);
        stat.setOpponent("Rivals");
        entityManager.persistAndFlush(stat);

        assertEquals(1, repository.findByPlayerId(3L).size());
        assertEquals(1, repository.findByCategoryId(9L).size());
        assertEquals(1, repository.findByOpponentContainingIgnoreCase("riv").size());
    }
}
