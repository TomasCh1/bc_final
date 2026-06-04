package com.basketball.app.controller;

import com.basketball.app.dto.EventCreateRequest;
import com.basketball.app.dto.EventUpdateRequest;
import com.basketball.app.model.Event;
import com.basketball.app.service.EventLiveActionService;
import com.basketball.app.service.EventNominationService;
import com.basketball.app.service.EventOpponentService;
import com.basketball.app.service.EventRefereeService;
import com.basketball.app.service.EventService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.*;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(value = EventController.class, excludeAutoConfiguration = SecurityAutoConfiguration.class)
class EventControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private EventService eventService;
    @MockBean
    private EventNominationService eventNominationService;
    @MockBean
    private EventOpponentService eventOpponentService;
    @MockBean
    private EventLiveActionService eventLiveActionService;
    @MockBean
    private EventRefereeService eventRefereeService;

    @MockBean
    private com.basketball.app.security.JwtTokenProvider jwtTokenProvider;

    @MockBean
    private com.basketball.app.security.CustomUserDetailsService userDetailsService;

    @Autowired
    private ObjectMapper objectMapper;

    private Event testEvent;
    private Map<String, Object> eventMap;

    @BeforeEach
    void setUp() {
        testEvent = new Event();
        testEvent.setId(1L);
        testEvent.setName("Training Session");
        testEvent.setType(Event.EventType.TRAINING);
        testEvent.setDate(LocalDate.now().plusDays(1));
        testEvent.setTime(LocalTime.of(18, 0));
        testEvent.setLocation("Gym A");

        eventMap = new HashMap<>();
        eventMap.put("id", 1L);
        eventMap.put("name", "Training Session");
        eventMap.put("type", "TRAINING");
        eventMap.put("date", testEvent.getDate().toString());
    }

    @Test
    void testCreateEvent_Success() throws Exception {
        EventCreateRequest request = new EventCreateRequest();
        request.setName("New Training");
        request.setType(Event.EventType.TRAINING);
        request.setDate(LocalDate.now().plusDays(2));
        request.setTime(LocalTime.of(19, 0));
        request.setLocation("Gym B");
        request.setCategoryId(1L);

        Map<String, Object> result = new HashMap<>();
        result.put("event", eventMap);
        result.put("count", 1);
        result.put("isRecurring", false);

        when(eventService.createEventWithDetails(any(EventCreateRequest.class))).thenReturn(result);

        mockMvc.perform(post("/api/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1L));

        verify(eventService).createEventWithDetails(any(EventCreateRequest.class));
    }

    @Test
    void testGetAllEvents_Success() throws Exception {
        List<Event> events = Arrays.asList(testEvent);
        when(eventService.findAll(any(), any(), any(), any(), any())).thenReturn(events);
        when(eventService.toEventMap(any(Event.class), anyBoolean())).thenReturn(eventMap);

        mockMvc.perform(get("/api/events"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1L))
                .andExpect(jsonPath("$[0].name").value("Training Session"));

        verify(eventService).findAll(isNull(), isNull(), isNull(), isNull(), isNull());
    }

    @Test
    void testGetAllEvents_WithFilters() throws Exception {
        List<Event> events = Arrays.asList(testEvent);
        when(eventService.findAll(any(), any(), any(), any(), any())).thenReturn(events);
        when(eventService.toEventMap(any(Event.class), anyBoolean())).thenReturn(eventMap);

        mockMvc.perform(get("/api/events")
                        .param("type", "TRAINING")
                        .param("categoryId", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].type").value("TRAINING"));

        verify(eventService).findAll(eq(1L), eq(Event.EventType.TRAINING), isNull(), isNull(), isNull());
    }

    @Test
    void testGetEventById_Success() throws Exception {
        when(eventService.findById(1L)).thenReturn(Optional.of(testEvent));
        when(eventService.toEventMap(any(Event.class))).thenReturn(eventMap);

        mockMvc.perform(get("/api/events/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.name").value("Training Session"));

        verify(eventService).findById(1L);
    }

    @Test
    void testGetEventById_NotFound() throws Exception {
        when(eventService.findById(999L)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/events/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").exists());

        verify(eventService).findById(999L);
    }

    @Test
    void testUpdateEvent_Success() throws Exception {
        EventUpdateRequest request = new EventUpdateRequest();
        request.setName("Updated Training");

        Event updatedEvent = new Event();
        updatedEvent.setId(1L);
        updatedEvent.setName("Updated Training");

        when(eventService.updateEvent(eq(1L), any(EventUpdateRequest.class))).thenReturn(updatedEvent);
        when(eventService.toEventMap(any(Event.class))).thenReturn(eventMap);

        mockMvc.perform(put("/api/events/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L));

        verify(eventService).updateEvent(eq(1L), any(EventUpdateRequest.class));
    }

    @Test
    void testDeleteEvent_Success() throws Exception {
        doNothing().when(eventService).deleteEvent(1L);

        mockMvc.perform(delete("/api/events/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Event deleted successfully"));

        verify(eventService).deleteEvent(1L);
    }

    @Test
    void testGetAllEvents_InvalidType_ReturnsBadRequest() throws Exception {
        mockMvc.perform(get("/api/events").param("type", "INVALID"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Invalid event type: INVALID"));

        verify(eventService, never()).findAll(any(), any(), any(), any(), any());
    }

    @Test
    void testCreateEvent_Recurring_ReturnsFullResult() throws Exception {
        EventCreateRequest request = new EventCreateRequest();
        request.setName("Weekly");
        request.setType(Event.EventType.TRAINING);
        request.setDate(LocalDate.now());
        request.setTime(LocalTime.of(18, 0));
        request.setCategoryId(1L);
        request.setIsRecurring(true);
        request.setRecurrencePattern(EventCreateRequest.RecurrencePattern.WEEKLY);
        request.setNumberOfOccurrences(4);

        Map<String, Object> result = new HashMap<>();
        result.put("event", eventMap);
        result.put("count", 4);
        result.put("isRecurring", true);
        when(eventService.createEventWithDetails(any())).thenReturn(result);

        mockMvc.perform(post("/api/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.count").value(4))
                .andExpect(jsonPath("$.isRecurring").value(true));
    }

    @Test
    void testGetEventsByCategory_Success() throws Exception {
        when(eventService.findByCategory(1L)).thenReturn(List.of(testEvent));
        when(eventService.toEventMap(any(Event.class))).thenReturn(eventMap);

        mockMvc.perform(get("/api/events/category/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1));
    }

    @Test
    void testGetEventsByType_Success() throws Exception {
        when(eventService.findByType(Event.EventType.TRAINING)).thenReturn(List.of(testEvent));
        when(eventService.toEventMap(any(Event.class))).thenReturn(eventMap);

        mockMvc.perform(get("/api/events/type/TRAINING"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].type").value("TRAINING"));
    }

    @Test
    void testGetEventsByType_InvalidType_ReturnsBadRequest() throws Exception {
        mockMvc.perform(get("/api/events/type/INVALID"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Invalid event type: INVALID"));
    }

    @Test
    void testGetEventsByDateRange_Success() throws Exception {
        LocalDate start = LocalDate.now();
        LocalDate end = LocalDate.now().plusDays(7);
        when(eventService.findByDateRange(start, end)).thenReturn(List.of(testEvent));
        when(eventService.toEventMap(any(Event.class))).thenReturn(eventMap);

        mockMvc.perform(get("/api/events/range")
                        .param("startDate", start.toString())
                        .param("endDate", end.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1));
    }

    @Test
    void testGetClosestMatch_Found_ReturnsEvent() throws Exception {
        when(eventService.findClosestMatch(any(), isNull())).thenReturn(Optional.of(testEvent));
        when(eventService.toEventMap(testEvent)).thenReturn(eventMap);

        mockMvc.perform(get("/api/events/closest-match"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    void testGetClosestMatch_NotFound_ReturnsEmptyObject() throws Exception {
        when(eventService.findClosestMatch(any(), isNull())).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/events/closest-match"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    void testGetEventNominations_Success() throws Exception {
        when(eventNominationService.getNominationsForEvent(1L)).thenReturn(Map.of(
                "eventId", 1L,
                "playerIds", List.of(5L),
                "nominations", List.of(Map.of("playerId", 5))
        ));

        mockMvc.perform(get("/api/events/1/nominations"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nominations[0].playerId").value(5));
    }

    @Test
    void testReplaceEventNominations_Success() throws Exception {
        when(eventNominationService.replaceNominationsForEvent(eq(1L), any(), any()))
                .thenReturn(Map.of("eventId", 1L, "playerIds", List.of(5L, 6L)));

        mockMvc.perform(put("/api/events/1/nominations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"playerIds\":[5,6]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.eventId").value(1));
    }

    @Test
    void testGetEventReferees_Success() throws Exception {
        when(eventRefereeService.getRefereesForEvent(1L)).thenReturn(Map.of(
                "eventId", 1L,
                "referees", List.of(Map.of("name", "Ref A"))
        ));

        mockMvc.perform(get("/api/events/1/referees"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.referees[0].name").value("Ref A"));
    }

    @Test
    void testGetEventOpponents_Success() throws Exception {
        when(eventOpponentService.getOpponentsForEvent(1L)).thenReturn(Map.of(
                "eventId", 1L,
                "jerseyNumbers", List.of(7)
        ));

        mockMvc.perform(get("/api/events/1/opponents"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.jerseyNumbers[0]").value(7));
    }

    @Test
    void testGetEventLiveActions_Success() throws Exception {
        when(eventLiveActionService.getLiveActions(1L)).thenReturn(List.of(Map.of("id", 10)));

        mockMvc.perform(get("/api/events/1/live-actions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(10));
    }

    @Test
    void testGetEventLiveSummary_Success() throws Exception {
        when(eventLiveActionService.getLiveSummary(1L)).thenReturn(Map.of("ourScore", 55, "opponentScore", 48));

        mockMvc.perform(get("/api/events/1/live-summary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ourScore").value(55));
    }

    @Test
    void testGetSeasonSummary_Success() throws Exception {
        LocalDate start = LocalDate.of(2026, 1, 1);
        LocalDate end = LocalDate.of(2026, 12, 31);
        when(eventService.getSeasonSummary(start, end, 1L))
                .thenReturn(Map.of("wins", 5, "losses", 3, "winPercent", 62.5));

        mockMvc.perform(get("/api/events/season-summary")
                        .param("startDate", start.toString())
                        .param("endDate", end.toString())
                        .param("categoryId", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.wins").value(5))
                .andExpect(jsonPath("$.losses").value(3));
    }

    @Test
    void testUpdateEvent_ServiceError_ReturnsBadRequest() throws Exception {
        when(eventService.updateEvent(eq(1L), any())).thenThrow(new RuntimeException("invalid update"));

        mockMvc.perform(put("/api/events/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Bad\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("invalid update"));
    }
}
