package com.basketball.app.service;

import com.basketball.app.dto.EventLiveActionCreateRequest;
import com.basketball.app.model.Event;
import com.basketball.app.model.EventLiveAction;
import com.basketball.app.repository.EventLiveActionRepository;
import com.basketball.app.repository.EventOpponentPlayerRepository;
import com.basketball.app.repository.EventRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@Tag("unit")
class EventLiveActionServiceTest {

    @Mock
    private EventRepository eventRepository;
    @Mock
    private EventLiveActionRepository liveActionRepository;
    @Mock
    private EventOpponentPlayerRepository opponentPlayerRepository;

    private EventLiveActionService service;

    @BeforeEach
    void setUp() {
        service = new EventLiveActionService(
                eventRepository, liveActionRepository, opponentPlayerRepository, new ObjectMapper()
        );
    }

    private Event matchEvent(long id) {
        Event e = new Event();
        e.setId(id);
        e.setType(Event.EventType.MATCH);
        return e;
    }

    @Test
    void createLiveAction_InvalidOpponentScoreWithoutJersey_Throws() {
        when(eventRepository.findById(5L)).thenReturn(Optional.of(matchEvent(5L)));

        EventLiveActionCreateRequest request = new EventLiveActionCreateRequest();
        request.setPeriod(1);
        request.setClockSeconds(120);
        request.setTeamSide(EventLiveAction.TeamSide.OPPONENT);
        request.setActionType(EventLiveAction.ActionType.SCORE);
        request.setPoints(2);

        assertThrows(RuntimeException.class, () -> service.createLiveAction(5L, request));
    }

    @Test
    void getLiveSummary_ComputesScoresAndDiff() {
        when(eventRepository.findById(9L)).thenReturn(Optional.of(matchEvent(9L)));

        EventLiveAction ourScore = new EventLiveAction();
        ourScore.setActionType(EventLiveAction.ActionType.SCORE);
        ourScore.setTeamSide(EventLiveAction.TeamSide.OUR);
        ourScore.setPoints(3);

        EventLiveAction oppScore = new EventLiveAction();
        oppScore.setActionType(EventLiveAction.ActionType.SCORE);
        oppScore.setTeamSide(EventLiveAction.TeamSide.OPPONENT);
        oppScore.setPoints(2);
        oppScore.setOpponentJerseyNumber(11);

        when(liveActionRepository.findByEventIdOrderByCreatedAtAscIdAsc(9L)).thenReturn(List.of(ourScore, oppScore));

        var summary = service.getLiveSummary(9L);
        assertEquals(3, summary.get("ourScore"));
        assertEquals(2, summary.get("opponentScore"));
        assertEquals(1, summary.get("scoreDiff"));
    }

    @Test
    void deleteLiveAction_MissingAction_Throws() {
        when(eventRepository.findById(10L)).thenReturn(Optional.of(matchEvent(10L)));
        when(liveActionRepository.findByIdAndEventId(77L, 10L)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> service.deleteLiveAction(10L, 77L));
    }
}
