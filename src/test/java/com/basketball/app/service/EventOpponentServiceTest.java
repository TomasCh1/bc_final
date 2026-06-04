package com.basketball.app.service;

import com.basketball.app.model.Event;
import com.basketball.app.model.EventLiveAction;
import com.basketball.app.model.EventOpponentPlayer;
import com.basketball.app.repository.EventLiveActionRepository;
import com.basketball.app.repository.EventOpponentPlayerRepository;
import com.basketball.app.repository.EventRepository;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@Tag("unit")
class EventOpponentServiceTest {

    @Mock
    private EventRepository eventRepository;
    @Mock
    private EventOpponentPlayerRepository eventOpponentPlayerRepository;
    @Mock
    private EventLiveActionRepository eventLiveActionRepository;

    @InjectMocks
    private EventOpponentService service;

    @Test
    void getOpponentsForEvent_MergesRosterAndActions() {
        Event event = new Event();
        event.setId(2L);
        event.setType(Event.EventType.MATCH);
        when(eventRepository.findById(2L)).thenReturn(Optional.of(event));

        EventOpponentPlayer p = new EventOpponentPlayer();
        p.setJerseyNumber(10);
        when(eventOpponentPlayerRepository.findByEventIdOrderByJerseyNumberAsc(2L)).thenReturn(List.of(p));

        EventLiveAction action = new EventLiveAction();
        action.setOpponentJerseyNumber(12);
        when(eventLiveActionRepository.findByEventIdOrderByCreatedAtAscIdAsc(2L)).thenReturn(List.of(action));

        var out = service.getOpponentsForEvent(2L);
        assertEquals(List.of(10, 12), out.get("jerseyNumbers"));
    }

    @Test
    void replaceOpponentsForEvent_InvalidJersey_Throws() {
        Event event = new Event();
        event.setId(3L);
        event.setType(Event.EventType.MATCH);
        when(eventRepository.findById(3L)).thenReturn(Optional.of(event));

        assertThrows(RuntimeException.class, () -> service.replaceOpponentsForEvent(3L, List.of(-1, 5)));
    }

    @Test
    void replaceOpponentsForEvent_AddsNewNumbers() {
        Event event = new Event();
        event.setId(4L);
        event.setType(Event.EventType.MATCH);
        when(eventRepository.findById(4L)).thenReturn(Optional.of(event));
        when(eventOpponentPlayerRepository.findByEventIdOrderByJerseyNumberAsc(4L)).thenReturn(List.of());
        when(eventLiveActionRepository.findByEventIdOrderByCreatedAtAscIdAsc(4L)).thenReturn(List.of());
        when(eventOpponentPlayerRepository.existsByEventIdAndJerseyNumber(4L, 9)).thenReturn(false);

        service.replaceOpponentsForEvent(4L, List.of(9));

        verify(eventOpponentPlayerRepository).save(any(EventOpponentPlayer.class));
    }
}
