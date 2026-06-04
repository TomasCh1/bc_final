package com.basketball.app.service;

import com.basketball.app.dto.EventNominationItemRequest;
import com.basketball.app.model.Event;
import com.basketball.app.model.EventNominatedPlayer;
import com.basketball.app.model.User;
import com.basketball.app.repository.EventNominatedPlayerRepository;
import com.basketball.app.repository.EventRepository;
import com.basketball.app.repository.UserRepository;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyIterable;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@Tag("unit")
class EventNominationServiceTest {

    @Mock
    private EventRepository eventRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private EventNominatedPlayerRepository nominatedPlayerRepository;

    @InjectMocks
    private EventNominationService service;

    @Test
    void replaceNominationsForEvent_DuplicateJersey_Throws() {
        Event event = new Event();
        event.setId(10L);
        event.setType(Event.EventType.MATCH);
        when(eventRepository.findById(10L)).thenReturn(Optional.of(event));

        EventNominationItemRequest n1 = new EventNominationItemRequest();
        n1.setPlayerId(1L);
        n1.setJerseyNumber(8);
        EventNominationItemRequest n2 = new EventNominationItemRequest();
        n2.setPlayerId(2L);
        n2.setJerseyNumber(8);

        assertThrows(RuntimeException.class,
                () -> service.replaceNominationsForEvent(10L, null, List.of(n1, n2)));
    }

    @Test
    void getNominationsForEvent_ReturnsMappedPayload() {
        Event event = new Event();
        event.setId(11L);
        event.setType(Event.EventType.MATCH);
        when(eventRepository.findById(11L)).thenReturn(Optional.of(event));

        EventNominatedPlayer row = new EventNominatedPlayer();
        row.setEventId(11L);
        row.setPlayerId(4L);
        row.setJerseyNumber(12);
        row.setStartingFive(true);
        when(nominatedPlayerRepository.findByEventId(11L)).thenReturn(List.of(row));

        User user = new User();
        user.setId(4L);
        user.setName("Player");
        user.setSurname("Four");
        when(userRepository.findAllById(anyIterable())).thenReturn(List.of(user));

        var result = service.getNominationsForEvent(11L);

        assertEquals(11L, result.get("eventId"));
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> nominations = (List<Map<String, Object>>) result.get("nominations");
        assertEquals(1, nominations.size());
        assertEquals("Player Four", nominations.get(0).get("playerName"));
    }
}
