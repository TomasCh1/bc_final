package com.basketball.app.repository;

import com.basketball.app.model.EventOpponentPlayer;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest(showSql = false)
@ActiveProfiles("test")
@Tag("integration")
class EventOpponentPlayerRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;
    @Autowired
    private EventOpponentPlayerRepository repository;

    @Test
    void findByEventIdOrderByJerseyNumberAsc_ReturnsSorted() {
        EventOpponentPlayer p1 = new EventOpponentPlayer();
        p1.setEventId(1L);
        p1.setJerseyNumber(12);
        EventOpponentPlayer p2 = new EventOpponentPlayer();
        p2.setEventId(1L);
        p2.setJerseyNumber(8);
        entityManager.persistAndFlush(p1);
        entityManager.persistAndFlush(p2);

        List<EventOpponentPlayer> rows = repository.findByEventIdOrderByJerseyNumberAsc(1L);
        assertEquals(8, rows.get(0).getJerseyNumber());
        assertTrue(repository.existsByEventIdAndJerseyNumber(1L, 12));
    }
}
