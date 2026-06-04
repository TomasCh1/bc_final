package com.basketball.app.repository;

import com.basketball.app.model.EventNominatedPlayer;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DataJpaTest(showSql = false)
@ActiveProfiles("test")
@Tag("integration")
class EventNominatedPlayerRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;
    @Autowired
    private EventNominatedPlayerRepository repository;

    @Test
    void findByEventIdAndDeleteByEventId_Work() {
        EventNominatedPlayer row = new EventNominatedPlayer();
        row.setEventId(10L);
        row.setPlayerId(2L);
        row.setJerseyNumber(7);
        row.setStartingFive(true);
        entityManager.persistAndFlush(row);

        assertEquals(1, repository.findByEventId(10L).size());
        repository.deleteByEventId(10L);
        entityManager.flush();
        assertEquals(0, repository.findByEventId(10L).size());
    }
}
