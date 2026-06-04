package com.basketball.app.service;

import com.basketball.app.dto.EventCreateRequest;
import com.basketball.app.dto.EventUpdateRequest;
import com.basketball.app.model.Category;
import com.basketball.app.model.Event;
import com.basketball.app.repository.AttendanceRepository;
import com.basketball.app.repository.EventLiveActionRepository;
import com.basketball.app.repository.EventNominatedPlayerRepository;
import com.basketball.app.repository.EventOpponentPlayerRepository;
import com.basketball.app.repository.EventRepository;
import com.basketball.app.repository.EventRefereeRepository;
import com.basketball.app.repository.MatchResultProjection;
import com.basketball.app.repository.StatisticsRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@Tag("unit")
class EventServiceTest {

    @Mock
    private EventRepository eventRepository;

    @Mock
    private CategoryService categoryService;
    @Mock
    private EventNominatedPlayerRepository eventNominatedPlayerRepository;
    @Mock
    private EventOpponentPlayerRepository eventOpponentPlayerRepository;
    @Mock
    private EventRefereeRepository eventRefereeRepository;
    @Mock
    private EventLiveActionRepository eventLiveActionRepository;
    @Mock
    private AttendanceRepository attendanceRepository;
    @Mock
    private StatisticsRepository statisticsRepository;

    @InjectMocks
    private EventService eventService;

    private Event testEvent;
    private Category testCategory;

    @BeforeEach
    void setUp() {
        testEvent = new Event();
        testEvent.setId(1L);
        testEvent.setName("Training Session");
        testEvent.setType(Event.EventType.TRAINING);
        testEvent.setDate(LocalDate.now().plusDays(1));
        testEvent.setTime(LocalTime.of(18, 0));
        testEvent.setLocation("Gym A");
        testEvent.setCategoryId(1L);

        testCategory = new Category();
        testCategory.setId(1L);
        testCategory.setName("U13");
        testCategory.setSeason("2024-2025");
    }

    @Test
    void testCreateEvent_Success() {
        EventCreateRequest request = new EventCreateRequest();
        request.setName("New Training");
        request.setType(Event.EventType.TRAINING);
        request.setDate(LocalDate.now().plusDays(2));
        request.setTime(LocalTime.of(19, 0));
        request.setLocation("Gym B");
        request.setCategoryId(1L);

        Event savedEvent = new Event();
        savedEvent.setId(2L);
        savedEvent.setName("New Training");

        when(categoryService.findById(1L)).thenReturn(Optional.of(testCategory));
        when(eventRepository.save(any(Event.class))).thenReturn(savedEvent);

        Event result = eventService.createEvent(request);

        assertNotNull(result);
        verify(categoryService).findById(1L);
        verify(eventRepository).save(any(Event.class));
    }

    @Test
    void testCreateEvent_CategoryNotFound() {
        EventCreateRequest request = new EventCreateRequest();
        request.setCategoryId(999L);

        when(categoryService.findById(999L)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> eventService.createEvent(request));
        verify(eventRepository, never()).save(any(Event.class));
    }

    @Test
    void testCreateEvent_WithoutCategory() {
        EventCreateRequest request = new EventCreateRequest();
        request.setName("Training");
        request.setType(Event.EventType.TRAINING);
        request.setDate(LocalDate.now());
        request.setTime(LocalTime.of(18, 0));
        request.setCategoryId(1L);

        Event savedEvent = new Event();
        savedEvent.setId(2L);

        when(categoryService.findById(1L)).thenReturn(Optional.of(testCategory));
        when(eventRepository.save(any(Event.class))).thenReturn(savedEvent);

        Event result = eventService.createEvent(request);

        assertNotNull(result);
        verify(categoryService).findById(1L);
        verify(eventRepository).save(any(Event.class));
    }

    @Test
    void testCreateEvent_MatchWithOpponent() {
        EventCreateRequest request = new EventCreateRequest();
        request.setName("Match");
        request.setType(Event.EventType.MATCH);
        request.setDate(LocalDate.now().plusDays(7));
        request.setOpponent("Team A");
        request.setCategoryId(1L);

        Event savedEvent = new Event();
        savedEvent.setId(2L);
        savedEvent.setOpponent("Team A");

        when(categoryService.findById(1L)).thenReturn(Optional.of(testCategory));
        when(eventRepository.save(any(Event.class))).thenReturn(savedEvent);

        Event result = eventService.createEvent(request);

        assertNotNull(result);
        verify(eventRepository).save(any(Event.class));
    }

    @Test
    void testUpdateEvent_Success() {
        EventUpdateRequest request = new EventUpdateRequest();
        request.setName("Updated Training");
        request.setLocation("New Location");

        Event updatedEvent = new Event();
        updatedEvent.setId(1L);
        updatedEvent.setName("Updated Training");
        updatedEvent.setLocation("New Location");

        when(eventRepository.findById(1L)).thenReturn(Optional.of(testEvent));
        when(eventRepository.save(any(Event.class))).thenReturn(updatedEvent);

        Event result = eventService.updateEvent(1L, request);

        assertNotNull(result);
        assertEquals("Updated Training", result.getName());
        verify(eventRepository).findById(1L);
        verify(eventRepository).save(any(Event.class));
    }

    @Test
    void testUpdateEvent_NotFound() {
        EventUpdateRequest request = new EventUpdateRequest();
        when(eventRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> eventService.updateEvent(999L, request));
    }

    @Test
    void testUpdateEvent_CategoryNotFound() {
        EventUpdateRequest request = new EventUpdateRequest();
        request.setCategoryId(999L);

        when(eventRepository.findById(1L)).thenReturn(Optional.of(testEvent));
        when(categoryService.findById(999L)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> eventService.updateEvent(1L, request));
    }

    @Test
    void testDeleteEvent_Success() {
        when(eventRepository.existsById(1L)).thenReturn(true);
        doNothing().when(eventRepository).deleteById(1L);

        eventService.deleteEvent(1L);

        verify(eventRepository).existsById(1L);
        verify(eventRepository).deleteById(1L);
    }

    @Test
    void testDeleteEvent_NotFound() {
        when(eventRepository.existsById(999L)).thenReturn(false);

        assertThrows(RuntimeException.class, () -> eventService.deleteEvent(999L));
        verify(eventRepository, never()).deleteById(anyLong());
    }

    @Test
    void testFindById_Success() {
        when(eventRepository.findById(1L)).thenReturn(Optional.of(testEvent));

        Optional<Event> result = eventService.findById(1L);

        assertTrue(result.isPresent());
        assertEquals(1L, result.get().getId());
        verify(eventRepository).findById(1L);
    }

    @Test
    void testFindById_NotFound() {
        when(eventRepository.findById(999L)).thenReturn(Optional.empty());

        Optional<Event> result = eventService.findById(999L);

        assertFalse(result.isPresent());
    }

    @Test
    void testFindAll_NoFilters() {
        List<Event> events = Arrays.asList(testEvent);
        when(eventRepository.findAll()).thenReturn(events);

        List<Event> result = eventService.findAll(null, null, null, null, null);

        assertEquals(1, result.size());
        verify(eventRepository).findAll();
    }

    @Test
    void testFindAll_WithCategoryFilter() {
        Event event1 = new Event();
        event1.setId(1L);
        event1.setCategoryId(1L);
        Event event2 = new Event();
        event2.setId(2L);
        event2.setCategoryId(2L);

        List<Event> allEvents = Arrays.asList(event1, event2);
        when(eventRepository.findAll()).thenReturn(allEvents);

        List<Event> result = eventService.findAll(1L, null, null, null, null);

        assertEquals(1, result.size());
        assertEquals(1L, result.get(0).getCategoryId());
    }

    @Test
    void testFindAll_WithTypeFilter() {
        Event training = new Event();
        training.setId(1L);
        training.setType(Event.EventType.TRAINING);
        Event match = new Event();
        match.setId(2L);
        match.setType(Event.EventType.MATCH);

        List<Event> allEvents = Arrays.asList(training, match);
        when(eventRepository.findAll()).thenReturn(allEvents);

        List<Event> result = eventService.findAll(null, Event.EventType.TRAINING, null, null, null);

        assertEquals(1, result.size());
        assertEquals(Event.EventType.TRAINING, result.get(0).getType());
    }

    @Test
    void testFindAll_WithDateRangeFilter() {
        LocalDate start = LocalDate.now();
        LocalDate end = LocalDate.now().plusDays(10);

        Event event1 = new Event();
        event1.setId(1L);
        event1.setType(Event.EventType.TRAINING);
        event1.setDate(LocalDate.now().plusDays(5));
        Event event2 = new Event();
        event2.setId(2L);
        event2.setType(Event.EventType.TRAINING);
        event2.setDate(LocalDate.now().plusDays(15));

        List<Event> allEvents = Arrays.asList(event1, event2);
        when(eventRepository.findByDateBetween(start, end)).thenReturn(List.of(event1));

        List<Event> result = eventService.findAll(null, null, start, end, null);

        assertEquals(1, result.size());
        assertTrue(result.get(0).getDate().isAfter(start.minusDays(1)) && 
                   result.get(0).getDate().isBefore(end.plusDays(1)));
    }

    @Test
    void testFindAll_WithSearchFilter() {
        Event event1 = new Event();
        event1.setId(1L);
        event1.setName("Basketball Training");
        event1.setLocation("Main Gym");
        Event event2 = new Event();
        event2.setId(2L);
        event2.setName("Soccer Match");
        event2.setLocation("Field");

        List<Event> allEvents = Arrays.asList(event1, event2);
        when(eventRepository.findAll()).thenReturn(allEvents);

        List<Event> result = eventService.findAll(null, null, null, null, "Basketball");

        assertEquals(1, result.size());
        assertTrue(result.get(0).getName().contains("Basketball"));
    }

    @Test
    void testFindByCategory() {
        List<Event> events = Arrays.asList(testEvent);
        when(eventRepository.findByCategoryId(1L)).thenReturn(events);

        List<Event> result = eventService.findByCategory(1L);

        assertEquals(1, result.size());
        verify(eventRepository).findByCategoryId(1L);
    }

    @Test
    void testFindAll_SearchByLocationAndOpponent() {
        Event byLocation = new Event();
        byLocation.setId(1L);
        byLocation.setLocation("Trnava Arena");
        Event byOpponent = new Event();
        byOpponent.setId(2L);
        byOpponent.setOpponent("Spartak");
        Event other = new Event();
        other.setId(3L);
        other.setName("Other");
        when(eventRepository.findAll()).thenReturn(List.of(byLocation, byOpponent, other));

        assertEquals(1, eventService.findAll(null, null, null, null, "trnava").size());
        assertEquals(1, eventService.findAll(null, null, null, null, "spartak").size());
    }

    @Test
    void testCreateEvent_TrainingWithoutTime_Throws() {
        EventCreateRequest request = new EventCreateRequest();
        request.setName("Training");
        request.setType(Event.EventType.TRAINING);
        request.setDate(LocalDate.now());
        request.setCategoryId(1L);
        when(categoryService.findById(1L)).thenReturn(Optional.of(testCategory));

        assertThrows(RuntimeException.class, () -> eventService.createEvent(request));
    }

    @Test
    void testCreateEvent_EndTimeBeforeStart_Throws() {
        EventCreateRequest request = new EventCreateRequest();
        request.setName("Training");
        request.setType(Event.EventType.TRAINING);
        request.setDate(LocalDate.now());
        request.setTime(LocalTime.of(18, 0));
        request.setEndTime(LocalTime.of(17, 0));
        request.setCategoryId(1L);
        when(categoryService.findById(1L)).thenReturn(Optional.of(testCategory));

        assertThrows(RuntimeException.class, () -> eventService.createEvent(request));
    }

    @Test
    void testCreateEvent_MatchInTrnava_SetsHomeMatch() {
        EventCreateRequest request = new EventCreateRequest();
        request.setName("Home Match");
        request.setType(Event.EventType.MATCH);
        request.setDate(LocalDate.now().plusDays(3));
        request.setTime(LocalTime.of(17, 0));
        request.setLocation("Trnava Sports Hall");
        request.setCategoryId(1L);

        when(categoryService.findById(1L)).thenReturn(Optional.of(testCategory));
        when(eventRepository.save(any(Event.class))).thenAnswer(i -> i.getArgument(0));

        Event result = eventService.createEvent(request);
        assertEquals(Boolean.TRUE, result.getIsHomeMatch());
    }

    @Test
    void testCreateEventWithDetails_RecurringTraining_ReturnsCount() {
        EventCreateRequest request = new EventCreateRequest();
        request.setName("Weekly Training");
        request.setType(Event.EventType.TRAINING);
        request.setDate(LocalDate.now());
        request.setTime(LocalTime.of(18, 0));
        request.setCategoryId(1L);
        request.setIsRecurring(true);
        request.setRecurrencePattern(EventCreateRequest.RecurrencePattern.WEEKLY);
        request.setNumberOfOccurrences(3);

        when(categoryService.findById(1L)).thenReturn(Optional.of(testCategory));
        when(eventRepository.saveAll(any())).thenAnswer(i -> i.getArgument(0));

        Map<String, Object> result = eventService.createEventWithDetails(request);

        assertEquals(true, result.get("isRecurring"));
        assertEquals(3, result.get("count"));
        verify(eventRepository).saveAll(any());
    }

    @Test
    void testFindClosestMatch_WithCategory() {
        when(eventRepository.findFirstByTypeAndCategoryIdAndDateGreaterThanEqualOrderByDateAscTimeAsc(
                eq(Event.EventType.MATCH), eq(1L), any(LocalDate.class)))
                .thenReturn(Optional.of(testEvent));

        Optional<Event> result = eventService.findClosestMatch(LocalDate.now(), 1L);

        assertTrue(result.isPresent());
    }

    @Test
    void testFindClosestMatch_WithoutCategory() {
        when(eventRepository.findFirstByTypeAndDateGreaterThanEqualOrderByDateAscTimeAsc(
                eq(Event.EventType.MATCH), any(LocalDate.class)))
                .thenReturn(Optional.of(testEvent));

        Optional<Event> result = eventService.findClosestMatch(LocalDate.now(), null);

        assertTrue(result.isPresent());
    }

    @Test
    void testGetSeasonSummary_ComputesWinsLossesAndBalances() {
        MatchResultProjection win = mock(MatchResultProjection.class);
        when(win.getCategoryId()).thenReturn(1L);
        when(win.getMatchWon()).thenReturn(true);
        when(win.getDate()).thenReturn(LocalDate.of(2026, 1, 10));

        MatchResultProjection loss = mock(MatchResultProjection.class);
        when(loss.getCategoryId()).thenReturn(1L);
        when(loss.getMatchWon()).thenReturn(false);
        when(loss.getDate()).thenReturn(LocalDate.of(2026, 1, 12));

        when(eventRepository.findMatchResultsBetween(eq(Event.EventType.MATCH), any(), any()))
                .thenReturn(List.of(win, loss));
        when(categoryService.findById(1L)).thenReturn(Optional.of(testCategory));

        Map<String, Object> summary = eventService.getSeasonSummary(
                LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31), null);

        assertEquals(1, summary.get("wins"));
        assertEquals(1, summary.get("losses"));
        assertEquals(2, summary.get("totalPlayed"));
        assertNotNull(summary.get("categoryBalances"));
        assertNotNull(summary.get("last5Results"));
    }

    @Test
    void testToEventMap_EnrichesCategory() {
        when(categoryService.findById(1L)).thenReturn(Optional.of(testCategory));

        Map<String, Object> map = eventService.toEventMap(testEvent, true);

        assertEquals("U13", map.get("categoryName"));
        assertEquals(1L, map.get("categoryId"));
    }

    @Test
    void testToEventMap_WithoutEnrich_SkipsCategoryLookup() {
        Map<String, Object> map = eventService.toEventMap(testEvent, false);

        assertNull(map.get("categoryName"));
        verify(categoryService, never()).findById(anyLong());
    }
}
