package com.basketball.app.repository;

import com.basketball.app.model.EventLiveAction;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DataJpaTest(showSql = false)
@ActiveProfiles("test")
@Tag("integration")
class EventLiveActionRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;
    @Autowired
    private EventLiveActionRepository repository;

    @Test
    void queryMethods_FilterByEventAndJersey() {
        EventLiveAction action = new EventLiveAction();
        action.setEventId(1L);
        action.setPeriod(1);
        action.setClockSeconds(100);
        action.setTeamSide(EventLiveAction.TeamSide.OPPONENT);
        action.setActionType(EventLiveAction.ActionType.SCORE);
        action.setOpponentJerseyNumber(23);
        action.setPoints(2);
        EventLiveAction saved = entityManager.persistAndFlush(action);

        assertEquals(1, repository.findByEventIdOrderByCreatedAtAscIdAsc(1L).size());
        assertEquals(1, repository.findByEventIdAndOpponentJerseyNumberIn(1L, List.of(23)).size());
        assertTrue(repository.findByIdAndEventId(saved.getId(), 1L).isPresent());
    }
}
