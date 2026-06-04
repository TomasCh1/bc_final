package com.basketball.app.service;

import com.basketball.app.dto.EventRefereeItemRequest;
import com.basketball.app.model.Event;
import com.basketball.app.model.EventReferee;
import com.basketball.app.repository.EventRefereeRepository;
import com.basketball.app.repository.EventRepository;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@Tag("unit")
class EventRefereeServiceTest {

    @Mock
    private EventRepository eventRepository;
    @Mock
    private EventRefereeRepository eventRefereeRepository;

    @InjectMocks
    private EventRefereeService service;

    @Test
    void getRefereesForEvent_ReturnsMappedRows() {
        Event event = new Event();
        event.setId(6L);
        event.setType(Event.EventType.MATCH);
        when(eventRepository.findById(6L)).thenReturn(Optional.of(event));

        EventReferee row = new EventReferee();
        row.setName("Ref A");
        row.setGrade("A");
        when(eventRefereeRepository.findByEventIdOrderBySortOrderAsc(6L)).thenReturn(List.of(row));

        var out = service.getRefereesForEvent(6L);
        assertEquals(1, ((List<?>) out.get("referees")).size());
    }

    @Test
    void replaceRefereesForEvent_TooMany_Throws() {
        Event event = new Event();
        event.setId(7L);
        event.setType(Event.EventType.MATCH);
        when(eventRepository.findById(7L)).thenReturn(Optional.of(event));

        List<EventRefereeItemRequest> requested = java.util.stream.IntStream.range(0, 6).mapToObj(i -> {
            EventRefereeItemRequest r = new EventRefereeItemRequest();
            r.setName("Ref " + i);
            r.setGrade("A");
            return r;
        }).toList();

        assertThrows(RuntimeException.class, () -> service.replaceRefereesForEvent(7L, requested));
    }

    @Test
    void replaceRefereesForEvent_ValidList_SavesRows() {
        Event event = new Event();
        event.setId(8L);
        event.setType(Event.EventType.MATCH);
        when(eventRepository.findById(8L)).thenReturn(Optional.of(event));

        EventRefereeItemRequest req = new EventRefereeItemRequest();
        req.setName("Ref 1");
        req.setGrade("B");

        service.replaceRefereesForEvent(8L, List.of(req));

        verify(eventRefereeRepository).saveAll(anyList());
    }
}
