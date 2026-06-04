package com.basketball.app.service;

import com.basketball.app.model.Event;
import com.basketball.app.repository.*;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@Tag("unit")
class MatchStatsTemplateExportServiceTest {

    @Mock
    private EventRepository eventRepository;
    @Mock
    private StatisticsRepository statisticsRepository;
    @Mock
    private EventNominatedPlayerRepository nominatedPlayerRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private CategoryRepository categoryRepository;
    @Mock
    private EventRefereeService eventRefereeService;

    @InjectMocks
    private MatchStatsTemplateExportService service;

    @Test
    void exportMatchTemplate_NullEventId_Throws() {
        assertThrows(RuntimeException.class, () -> service.exportMatchTemplate(null, "Test User"));
    }

    @Test
    void exportMatchTemplate_NotMatchEvent_Throws() {
        Event training = new Event();
        training.setId(99L);
        training.setType(Event.EventType.TRAINING);
        when(eventRepository.findById(99L)).thenReturn(Optional.of(training));

        assertThrows(RuntimeException.class, () -> service.exportMatchTemplate(99L, "Test User"));
    }
}
