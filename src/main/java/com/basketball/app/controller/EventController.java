package com.basketball.app.controller;

import com.basketball.app.dto.EventCreateRequest;
import com.basketball.app.dto.EventLiveActionCreateRequest;
import com.basketball.app.dto.EventNominationsRequest;
import com.basketball.app.dto.EventOpponentsRequest;
import com.basketball.app.dto.EventRefereesRequest;
import com.basketball.app.dto.EventUpdateRequest;
import com.basketball.app.model.Event;
import com.basketball.app.service.EventLiveActionService;
import com.basketball.app.service.EventNominationService;
import com.basketball.app.service.EventOpponentService;
import com.basketball.app.service.EventRefereeService;
import com.basketball.app.service.EventService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/events")
public class EventController {

    private final EventService eventService;
    private final EventNominationService eventNominationService;
    private final EventOpponentService eventOpponentService;
    private final EventLiveActionService eventLiveActionService;
    private final EventRefereeService eventRefereeService;

    public EventController(
            EventService eventService,
            EventNominationService eventNominationService,
            EventOpponentService eventOpponentService,
            EventLiveActionService eventLiveActionService,
            EventRefereeService eventRefereeService
    ) {
        this.eventService = eventService;
        this.eventNominationService = eventNominationService;
        this.eventOpponentService = eventOpponentService;
        this.eventLiveActionService = eventLiveActionService;
        this.eventRefereeService = eventRefereeService;
    }

    /**
     * Get all events with optional filters
     */
    @GetMapping
    public ResponseEntity<?> getAllEvents(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false, defaultValue = "true") boolean enrich) {
        try {
            Event.EventType eventType = null;
            if (type != null && !type.trim().isEmpty()) {
                try {
                    eventType = Event.EventType.valueOf(type.toUpperCase());
                } catch (IllegalArgumentException e) {
                    return ResponseEntity.badRequest()
                            .body(Map.of("error", "Invalid event type: " + type));
                }
            }

            List<Event> events = eventService.findAll(categoryId, eventType, startDate, endDate, search);
            List<Map<String, Object>> eventMaps = events.stream()
                    .map(e -> eventService.toEventMap(e, enrich))
                    .collect(java.util.stream.Collectors.toList());

            return ResponseEntity.ok(eventMaps);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error fetching events: " + e.getMessage()));
        }
    }

    /**
     * Get event by ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<?> getEventById(@PathVariable Long id) {
        try {
            Event event = eventService.findById(id)
                    .orElseThrow(() -> new RuntimeException("Event not found: " + id));
            
            return ResponseEntity.ok(eventService.toEventMap(event));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Create event
     */
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'TRAINER')")
    public ResponseEntity<?> createEvent(@Valid @RequestBody EventCreateRequest request) {
        try {
            // Use createEventWithDetails to get count for recurring events
            Map<String, Object> result = eventService.createEventWithDetails(request);
            
            // For backward compatibility, if it's not recurring, return just the event
            if (Boolean.FALSE.equals(result.get("isRecurring"))) {
                return ResponseEntity.status(HttpStatus.CREATED)
                        .body(result.get("event"));
            }
            
            // For recurring events, return the full result with count
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(result);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Update event
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'TRAINER')")
    public ResponseEntity<?> updateEvent(@PathVariable Long id, @Valid @RequestBody EventUpdateRequest request) {
        try {
            Event event = eventService.updateEvent(id, request);
            return ResponseEntity.ok(eventService.toEventMap(event));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Delete event
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'TRAINER')")
    public ResponseEntity<?> deleteEvent(@PathVariable Long id) {
        try {
            eventService.deleteEvent(id);
            return ResponseEntity.ok(Map.of("message", "Event deleted successfully"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Get events by category
     */
    @GetMapping("/category/{categoryId}")
    public ResponseEntity<?> getEventsByCategory(@PathVariable Long categoryId) {
        try {
            List<Event> events = eventService.findByCategory(categoryId);
            List<Map<String, Object>> eventMaps = events.stream()
                    .map(eventService::toEventMap)
                    .collect(java.util.stream.Collectors.toList());
            return ResponseEntity.ok(eventMaps);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Get events by type
     */
    @GetMapping("/type/{type}")
    public ResponseEntity<?> getEventsByType(@PathVariable String type) {
        try {
            Event.EventType eventType = Event.EventType.valueOf(type.toUpperCase());
            List<Event> events = eventService.findByType(eventType);
            List<Map<String, Object>> eventMaps = events.stream()
                    .map(eventService::toEventMap)
                    .collect(java.util.stream.Collectors.toList());
            return ResponseEntity.ok(eventMaps);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Invalid event type: " + type));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Get events by date range
     */
    @GetMapping("/range")
    public ResponseEntity<?> getEventsByDateRange(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        try {
            List<Event> events = eventService.findByDateRange(startDate, endDate);
            List<Map<String, Object>> eventMaps = events.stream()
                    .map(eventService::toEventMap)
                    .collect(java.util.stream.Collectors.toList());
            return ResponseEntity.ok(eventMaps);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Get the match closest to the given date (on or after it). Single event.
     */
    @GetMapping("/closest-match")
    public ResponseEntity<?> getClosestMatch(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required = false) Long categoryId) {
        try {
            LocalDate onOrAfter = date != null ? date : LocalDate.now();
            return eventService.findClosestMatch(onOrAfter, categoryId)
                    .map(event -> ResponseEntity.ok(eventService.toEventMap(event)))
                    .orElseGet(() -> ResponseEntity.ok(Map.of()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Get nominated players for a match
     */
    @GetMapping("/{id}/nominations")
    public ResponseEntity<?> getEventNominations(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(eventNominationService.getNominationsForEvent(id));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Replace nominated players for a match
     */
    @PutMapping("/{id}/nominations")
    @PreAuthorize("hasAnyRole('ADMIN', 'TRAINER')")
    public ResponseEntity<?> replaceEventNominations(
            @PathVariable Long id,
            @RequestBody(required = false) EventNominationsRequest request
    ) {
        try {
            List<Long> playerIds = request != null ? request.getPlayerIds() : null;
            return ResponseEntity.ok(eventNominationService.replaceNominationsForEvent(
                    id,
                    playerIds,
                    request != null ? request.getNominations() : null
            ));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Get referees for a match
     */
    @GetMapping("/{id}/referees")
    public ResponseEntity<?> getEventReferees(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(eventRefereeService.getRefereesForEvent(id));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Replace referees for a match (max 3). Admin and Trainer only.
     */
    @PutMapping("/{id}/referees")
    @PreAuthorize("hasAnyRole('ADMIN', 'TRAINER')")
    public ResponseEntity<?> replaceEventReferees(
            @PathVariable Long id,
            @RequestBody(required = false) EventRefereesRequest request
    ) {
        try {
            return ResponseEntity.ok(eventRefereeService.replaceRefereesForEvent(
                    id,
                    request != null ? request.getReferees() : null
            ));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Get opponent jersey numbers for a match
     */
    @GetMapping("/{id}/opponents")
    public ResponseEntity<?> getEventOpponents(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(eventOpponentService.getOpponentsForEvent(id));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Replace opponent jersey numbers for a match
     */
    @PutMapping("/{id}/opponents")
    @PreAuthorize("hasAnyRole('ADMIN', 'TRAINER')")
    public ResponseEntity<?> replaceEventOpponents(
            @PathVariable Long id,
            @RequestBody(required = false) EventOpponentsRequest request
    ) {
        try {
            return ResponseEntity.ok(eventOpponentService.replaceOpponentsForEvent(
                    id,
                    request != null ? request.getJerseyNumbers() : null
            ));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * List persisted live actions for a match
     */
    @GetMapping("/{id}/live-actions")
    public ResponseEntity<?> getEventLiveActions(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(eventLiveActionService.getLiveActions(id));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Create one persisted live action for a match
     */
    @PostMapping("/{id}/live-actions")
    @PreAuthorize("hasAnyRole('ADMIN', 'TRAINER')")
    public ResponseEntity<?> createEventLiveAction(
            @PathVariable Long id,
            @Valid @RequestBody EventLiveActionCreateRequest request
    ) {
        try {
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(eventLiveActionService.createLiveAction(id, request));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Delete persisted live action for a match
     */
    @DeleteMapping("/{id}/live-actions/{actionId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'TRAINER')")
    public ResponseEntity<?> deleteEventLiveAction(
            @PathVariable Long id,
            @PathVariable Long actionId
    ) {
        try {
            eventLiveActionService.deleteLiveAction(id, actionId);
            return ResponseEntity.ok(Map.of("message", "Live action deleted."));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Get live summary (score, V/P, opponent jerseys, player +/-) for a match.
     */
    @GetMapping("/{id}/live-summary")
    public ResponseEntity<?> getEventLiveSummary(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(eventLiveActionService.getLiveSummary(id));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Get season summary: wins, losses, win %, category balances, last 5 results.
     */
    @GetMapping("/season-summary")
    public ResponseEntity<?> getSeasonSummary(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) Long categoryId) {
        try {
            return ResponseEntity.ok(eventService.getSeasonSummary(startDate, endDate, categoryId));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
        }
    }
}

