package com.basketball.app.service;

import com.basketball.app.dto.EventCreateRequest;
import com.basketball.app.dto.EventUpdateRequest;
import com.basketball.app.model.Event;
import com.basketball.app.repository.AttendanceRepository;
import com.basketball.app.repository.EventRepository;
import com.basketball.app.repository.EventLiveActionRepository;
import com.basketball.app.repository.EventNominatedPlayerRepository;
import com.basketball.app.repository.EventOpponentPlayerRepository;
import com.basketball.app.repository.EventRefereeRepository;
import com.basketball.app.repository.MatchResultProjection;
import com.basketball.app.repository.StatisticsRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class EventService {

    private final EventRepository eventRepository;
    private final CategoryService categoryService;
    private final EventNominatedPlayerRepository eventNominatedPlayerRepository;
    private final EventOpponentPlayerRepository eventOpponentPlayerRepository;
    private final EventRefereeRepository eventRefereeRepository;
    private final EventLiveActionRepository eventLiveActionRepository;
    private final AttendanceRepository attendanceRepository;
    private final StatisticsRepository statisticsRepository;

    public EventService(
            EventRepository eventRepository,
            CategoryService categoryService,
            EventNominatedPlayerRepository eventNominatedPlayerRepository,
            EventOpponentPlayerRepository eventOpponentPlayerRepository,
            EventRefereeRepository eventRefereeRepository,
            EventLiveActionRepository eventLiveActionRepository,
            AttendanceRepository attendanceRepository,
            StatisticsRepository statisticsRepository) {
        this.eventRepository = eventRepository;
        this.categoryService = categoryService;
        this.eventNominatedPlayerRepository = eventNominatedPlayerRepository;
        this.eventOpponentPlayerRepository = eventOpponentPlayerRepository;
        this.eventRefereeRepository = eventRefereeRepository;
        this.eventLiveActionRepository = eventLiveActionRepository;
        this.attendanceRepository = attendanceRepository;
        this.statisticsRepository = statisticsRepository;
    }

    /**
     * Get all events with optional filters
     */
    public List<Event> findAll(Long categoryId, Event.EventType type, LocalDate startDate, LocalDate endDate, String search) {
        String searchTrimmed = search != null ? search.trim() : null;

        // Start from a reasonably filtered base set to keep performance good
        List<Event> results;
        if (startDate != null && endDate != null) {
            results = eventRepository.findByDateBetween(startDate, endDate);
        } else {
            results = eventRepository.findAll();
        }

        // Apply filters in memory – cheap on the already narrowed set
        if (categoryId != null) {
            results = results.stream()
                    .filter(e -> e.getCategoryId() != null && e.getCategoryId().equals(categoryId))
                    .collect(java.util.stream.Collectors.toList());
        }

        if (type != null) {
            results = results.stream()
                    .filter(e -> e.getType() == type)
                    .collect(java.util.stream.Collectors.toList());
        }

        if (searchTrimmed != null && !searchTrimmed.isEmpty()) {
            String searchLower = searchTrimmed.toLowerCase();
            results = results.stream()
                    .filter(e ->
                            (e.getName() != null && e.getName().toLowerCase().contains(searchLower)) ||
                            (e.getLocation() != null && e.getLocation().toLowerCase().contains(searchLower)) ||
                            (e.getOpponent() != null && e.getOpponent().toLowerCase().contains(searchLower))
                    )
                    .collect(java.util.stream.Collectors.toList());
        }

        return results;
    }

    /**
     * Get event by ID
     */
    public Optional<Event> findById(Long id) {
        return eventRepository.findById(id);
    }

    /**
     * Create a new event (or multiple events if recurring)
     */
    @Transactional
    public Event createEvent(EventCreateRequest request) {
        validateCategoryRequired(request.getCategoryId());
        validateCategoryExists(request.getCategoryId());

        validateTrainingStartTime(request.getType(), request.getTime());
        validateTimeWindow(request.getTime(), request.getEndTime());

        // Check if this is a recurring training event
        if (Boolean.TRUE.equals(request.getIsRecurring()) && 
            request.getType() == Event.EventType.TRAINING && 
            request.getRecurrencePattern() != null) {
            
            return createRecurringEvents(request);
        }

        // Create single event
        Event event = new Event();
        event.setName(request.getName());
        event.setType(request.getType());
        event.setDate(request.getDate());
        event.setTime(request.getTime());
        event.setEndTime(request.getEndTime());
        event.setLocation(request.getLocation());
        event.setCategoryId(request.getCategoryId());
        event.setOpponent(request.getOpponent());
        event.setNotes(request.getNotes());
        event.setCancelled(false);

        // Set isHomeMatch: use provided value, or auto-determine from location for MATCH type
        if (request.getIsHomeMatch() != null) {
            event.setIsHomeMatch(request.getIsHomeMatch());
        } else if (event.getType() == Event.EventType.MATCH && event.getLocation() != null) {
            event.setIsHomeMatch(determineIsHomeMatch(event.getLocation()));
        }
        
        // Set matchWon (only for MATCH type)
        if (event.getType() == Event.EventType.MATCH) {
            event.setMatchWon(request.getMatchWon());
        }

        return eventRepository.save(event);
    }

    /**
     * Create event and return creation result with count (for recurring events)
     */
    @Transactional
    public Map<String, Object> createEventWithDetails(EventCreateRequest request) {
        validateCategoryRequired(request.getCategoryId());
        validateCategoryExists(request.getCategoryId());

        validateTrainingStartTime(request.getType(), request.getTime());
        validateTimeWindow(request.getTime(), request.getEndTime());

        // Check if this is a recurring training event
        if (Boolean.TRUE.equals(request.getIsRecurring()) && 
            request.getType() == Event.EventType.TRAINING && 
            request.getRecurrencePattern() != null) {
            
            List<Event> createdEvents = createRecurringEventsList(request);
            Event firstEvent = createdEvents.isEmpty() ? null : createdEvents.get(0);
            
            Map<String, Object> result = new java.util.HashMap<>();
            result.put("event", firstEvent != null ? toEventMap(firstEvent) : null);
            result.put("count", createdEvents.size());
            result.put("isRecurring", true);
            return result;
        }

        // Create single event
        Event event = new Event();
        event.setName(request.getName());
        event.setType(request.getType());
        event.setDate(request.getDate());
        event.setTime(request.getTime());
        event.setEndTime(request.getEndTime());
        event.setLocation(request.getLocation());
        event.setCategoryId(request.getCategoryId());
        event.setOpponent(request.getOpponent());
        event.setNotes(request.getNotes());
        event.setCancelled(false);

        // Set isHomeMatch: use provided value, or auto-determine from location for MATCH type
        if (request.getIsHomeMatch() != null) {
            event.setIsHomeMatch(request.getIsHomeMatch());
        } else if (event.getType() == Event.EventType.MATCH && event.getLocation() != null) {
            event.setIsHomeMatch(determineIsHomeMatch(event.getLocation()));
        }
        
        // Set matchWon (only for MATCH type)
        if (event.getType() == Event.EventType.MATCH) {
            event.setMatchWon(request.getMatchWon());
        }

        Event savedEvent = eventRepository.save(event);
        
        Map<String, Object> result = new java.util.HashMap<>();
        result.put("event", toEventMap(savedEvent));
        result.put("count", 1);
        result.put("isRecurring", false);
        return result;
    }

    /**
     * Create multiple recurring events based on the recurrence pattern
     */
    private Event createRecurringEvents(EventCreateRequest request) {
        List<Event> events = createRecurringEventsList(request);
        return events.isEmpty() ? null : events.get(0);
    }

    /**
     * Create multiple recurring events and return the list
     */
    private List<Event> createRecurringEventsList(EventCreateRequest request) {
        List<Event> events = new ArrayList<>();
        LocalDate currentDate = request.getDate();
        LocalDate endDate = determineEndDate(request);
        int interval = request.getRecurrenceInterval() != null ? request.getRecurrenceInterval() : 1;
        int count = 0;
        int maxOccurrences = request.getNumberOfOccurrences() != null ? request.getNumberOfOccurrences() : Integer.MAX_VALUE;

        // Generate events based on recurrence pattern
        while (currentDate.isBefore(endDate) || currentDate.isEqual(endDate)) {
            if (count >= maxOccurrences) {
                break;
            }

            Event event = new Event();
            event.setName(request.getName());
            event.setType(request.getType());
            event.setDate(currentDate);
            event.setTime(request.getTime());
            event.setEndTime(request.getEndTime());
            event.setLocation(request.getLocation());
            event.setCategoryId(request.getCategoryId());
            event.setOpponent(request.getOpponent());
            event.setNotes(request.getNotes());
            event.setCancelled(false);

            // Set isHomeMatch: use provided value, or auto-determine from location for MATCH type
            if (request.getIsHomeMatch() != null) {
                event.setIsHomeMatch(request.getIsHomeMatch());
            } else if (event.getType() == Event.EventType.MATCH && event.getLocation() != null) {
                event.setIsHomeMatch(determineIsHomeMatch(event.getLocation()));
            }
            
            // Set matchWon (only for MATCH type)
            if (event.getType() == Event.EventType.MATCH) {
                event.setMatchWon(request.getMatchWon());
            }

            events.add(event);
            count++;

            // Calculate next date based on recurrence pattern
            currentDate = calculateNextDate(currentDate, request.getRecurrencePattern(), interval);
        }

        // Save all events
        return eventRepository.saveAll(events);
    }

    /**
     * Determine the end date for recurring events
     */
    private LocalDate determineEndDate(EventCreateRequest request) {
        if (request.getRecurrenceEndDate() != null) {
            return request.getRecurrenceEndDate();
        }
        
        if (request.getNumberOfOccurrences() != null && request.getNumberOfOccurrences() > 0) {
            // Calculate end date based on number of occurrences
            LocalDate startDate = request.getDate();
            int interval = request.getRecurrenceInterval() != null ? request.getRecurrenceInterval() : 1;
            int occurrences = request.getNumberOfOccurrences();
            
            LocalDate calculatedEndDate = startDate;
            for (int i = 1; i < occurrences; i++) {
                calculatedEndDate = calculateNextDate(calculatedEndDate, request.getRecurrencePattern(), interval);
            }
            return calculatedEndDate;
        }
        
        // Default: 1 year from start date
        return request.getDate().plusYears(1);
    }

    /**
     * Calculate the next date based on recurrence pattern
     */
    private LocalDate calculateNextDate(LocalDate currentDate, EventCreateRequest.RecurrencePattern pattern, int interval) {
        switch (pattern) {
            case DAILY:
                return currentDate.plusDays(interval);
            case WEEKLY:
                return currentDate.plusWeeks(interval);
            case MONTHLY:
                return currentDate.plusMonths(interval);
            default:
                return currentDate.plusWeeks(interval);
        }
    }

    /**
     * Update an existing event
     */
    @Transactional
    public Event updateEvent(Long id, EventUpdateRequest request) {
        Event event = eventRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Event not found: " + id));

        if (request.getCategoryId() != null) {
            validateCategoryExists(request.getCategoryId());
            event.setCategoryId(request.getCategoryId());
        }

        if (request.getName() != null) {
            event.setName(request.getName());
        }
        if (request.getType() != null) {
            event.setType(request.getType());
        }
        if (request.getDate() != null) {
            event.setDate(request.getDate());
        }
        if (request.getTime() != null) {
            event.setTime(request.getTime());
        }
        if (request.getEndTime() != null) {
            event.setEndTime(request.getEndTime());
        }
        if (request.getLocation() != null) {
            event.setLocation(request.getLocation());
        }
        if (request.getOpponent() != null) {
            event.setOpponent(request.getOpponent());
        }
        if (request.getNotes() != null) {
            event.setNotes(request.getNotes());
        }
        
        // Update isHomeMatch: use provided value, or auto-determine from location for MATCH type
        if (request.getIsHomeMatch() != null) {
            event.setIsHomeMatch(request.getIsHomeMatch());
        } else if (event.getType() == Event.EventType.MATCH && request.getLocation() != null) {
            event.setIsHomeMatch(determineIsHomeMatch(request.getLocation()));
        } else if (event.getType() == Event.EventType.MATCH && event.getLocation() != null && event.getIsHomeMatch() == null) {
            // Re-evaluate if location changed
            event.setIsHomeMatch(determineIsHomeMatch(event.getLocation()));
        }
        
        // Update matchWon (only for MATCH type)
        // Allow setting to null to mark as "not played yet"
        if (event.getType() == Event.EventType.MATCH) {
            // Check if matchWon field is present in the request (even if null)
            // We need to distinguish between "not provided" and "explicitly set to null"
            // Since Boolean can be null, we'll always update if it's a MATCH type
            // The frontend will send null when "not played yet" is selected
            event.setMatchWon(request.getMatchWon());
            if (request.getOurScore() != null) {
                event.setOurScore(request.getOurScore());
            }
            if (request.getOpponentScore() != null) {
                event.setOpponentScore(request.getOpponentScore());
            }
            // Auto-set matchWon from score when both scores are provided
            if (request.getOurScore() != null && request.getOpponentScore() != null) {
                //event.setMatchWon(request.getOurScore() > request.getOpponentScore());
            }
        }
        if (request.getCancelled() != null && event.getType() == Event.EventType.TRAINING) {
            event.setCancelled(request.getCancelled());
        }

        validateCategoryRequired(event.getCategoryId());
        validateTrainingStartTime(event.getType(), event.getTime());
        validateTimeWindow(event.getTime(), event.getEndTime());
        return eventRepository.save(event);
    }

    /**
     * Delete an event
     */
    @Transactional
    public void deleteEvent(Long id) {
        if (!eventRepository.existsById(id)) {
            throw new RuntimeException("Event not found: " + id);
        }
        // Explicitly remove all event-linked data first so admin deletion is complete
        // even when legacy DB constraints are missing CASCADE rules.
        eventNominatedPlayerRepository.deleteByEventId(id);
        eventOpponentPlayerRepository.deleteByEventId(id);
        eventRefereeRepository.deleteByEventId(id);
        eventLiveActionRepository.deleteByEventId(id);
        attendanceRepository.deleteByEventId(id);
        statisticsRepository.deleteByEventId(id);
        eventRepository.deleteById(id);
    }

    /**
     * Get events by category
     */
    public List<Event> findByCategory(Long categoryId) {
        return eventRepository.findByCategoryId(categoryId);
    }

    /**
     * Get events by type
     */
    public List<Event> findByType(Event.EventType type) {
        return eventRepository.findByType(type);
    }

    /**
     * Get events by date range
     */
    public List<Event> findByDateRange(LocalDate startDate, LocalDate endDate) {
        return eventRepository.findByDateBetween(startDate, endDate);
    }

    /**
     * Get the single match closest to the given date.
     * Optional category filter.
     */
    public Optional<Event> findClosestMatch(LocalDate onOrAfter, Long categoryId) {
        if (categoryId != null) {
            return eventRepository.findFirstByTypeAndCategoryIdAndDateGreaterThanEqualOrderByDateAscTimeAsc(
                    Event.EventType.MATCH, categoryId, onOrAfter);
        }
        return eventRepository.findFirstByTypeAndDateGreaterThanEqualOrderByDateAscTimeAsc(
                Event.EventType.MATCH, onOrAfter);
    }

    /**
     * Get season summary: wins, losses, win percent, per-category balances, last 5 results.
     */
    public Map<String, Object> getSeasonSummary(LocalDate startDate, LocalDate endDate, Long categoryId) {
        List<MatchResultProjection> matches = eventRepository.findMatchResultsBetween(
                Event.EventType.MATCH, startDate, endDate);

        List<MatchResultProjection> filtered = categoryId != null
                ? matches.stream().filter(m -> categoryId.equals(m.getCategoryId())).collect(Collectors.toList())
                : matches;

        int wins = 0, losses = 0;
        for (MatchResultProjection m : filtered) {
            if (Boolean.TRUE.equals(m.getMatchWon())) wins++;
            else if (Boolean.FALSE.equals(m.getMatchWon())) losses++;
        }
        int totalPlayed = wins + losses;
        double winPercent = totalPlayed > 0 ? (wins * 100.0 / totalPlayed) : 0;

        Map<Long, int[]> byCategory = new LinkedHashMap<>();
        for (MatchResultProjection m : matches) {
            Long cid = m.getCategoryId();
            if (cid == null) cid = -1L;
            byCategory.computeIfAbsent(cid, k -> new int[]{0, 0});
            if (Boolean.TRUE.equals(m.getMatchWon())) byCategory.get(cid)[0]++;
            else if (Boolean.FALSE.equals(m.getMatchWon())) byCategory.get(cid)[1]++;
        }
        List<Map<String, Object>> categoryBalances = new ArrayList<>();
        for (Map.Entry<Long, int[]> e : byCategory.entrySet()) {
            int w = e.getValue()[0], l = e.getValue()[1], games = w + l;
            if (games == 0) continue;
            String name = e.getKey() >= 0
                    ? categoryService.findById(e.getKey()).map(c -> c.getName()).orElse("?")
                    : "-";
            categoryBalances.add(Map.<String, Object>of(
                    "categoryId", e.getKey(),
                    "categoryName", name,
                    "wins", w,
                    "losses", l,
                    "games", games,
                    "balance", w - l
            ));
        }
        categoryBalances.sort((a, b) -> {
            int ba = (int) a.get("balance"), bb = (int) b.get("balance");
            if (ba != bb) return Integer.compare(bb, ba);
            int wa = (int) a.get("wins"), wb = (int) b.get("wins");
            if (wa != wb) return Integer.compare(wb, wa);
            return Integer.compare((int) b.get("games"), (int) a.get("games"));
        });

        List<Map<String, Object>> last5Results = filtered.stream()
                .filter(m -> m.getMatchWon() != null)
                .sorted(Comparator.comparing(MatchResultProjection::getDate).reversed())
                .limit(5)
                .map(m -> {
                    Map<String, Object> map = new LinkedHashMap<>();
                    map.put("date", m.getDate().toString());
                    map.put("won", m.getMatchWon());
                    map.put("categoryId", m.getCategoryId());
                    return map;
                })
                .collect(Collectors.toList());

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("wins", wins);
        out.put("losses", losses);
        out.put("totalPlayed", totalPlayed);
        out.put("winPercent", Math.round(winPercent * 10) / 10.0);
        out.put("categoryBalances", categoryBalances);
        out.put("last5Results", last5Results);
        return out;
    }

    /**
     * Determine if a match is a home match based on location
     * Returns true if location contains "Trnava" (case-insensitive), false otherwise
     */
    private Boolean determineIsHomeMatch(String location) {
        if (location == null || location.trim().isEmpty()) {
            return null;
        }
        return location.toLowerCase().contains("trnava");
    }

    private void validateCategoryRequired(Long categoryId) {
        if (categoryId == null) {
            throw new RuntimeException("Category is required for all events");
        }
    }

    private void validateCategoryExists(Long categoryId) {
        categoryService.findById(categoryId)
                .orElseThrow(() -> new RuntimeException("Category not found: " + categoryId));
    }

    /**
     * When recording a training, start time is required.
     */
    private void validateTrainingStartTime(Event.EventType type, LocalTime time) {
        if (type == Event.EventType.TRAINING && time == null) {
            throw new RuntimeException("Start time is required for training events");
        }
    }

    /**
     * When both start and end time are set, end must be after start.
     */
    private void validateTimeWindow(LocalTime start, LocalTime end) {
        if (start != null && end != null && !end.isAfter(start)) {
            throw new RuntimeException("End time must be after start time");
        }
    }

    /**
     * Convert Event to map with enriched data.
     * When enrich=false skips category/coach lookup for smaller payload.
     */
    public Map<String, Object> toEventMap(Event event) {
        return toEventMap(event, true);
    }

    public Map<String, Object> toEventMap(Event event, boolean enrich) {
        Map<String, Object> eventMap = new java.util.HashMap<>();
        eventMap.put("id", event.getId());
        eventMap.put("name", event.getName());
        eventMap.put("type", event.getType());
        eventMap.put("date", event.getDate());
        eventMap.put("time", event.getTime());
        eventMap.put("endTime", event.getEndTime());
        eventMap.put("location", event.getLocation());
        eventMap.put("categoryId", event.getCategoryId());
        
        if (enrich && event.getCategoryId() != null) {
            try {
                categoryService.findById(event.getCategoryId()).ifPresent(category -> {
                    eventMap.put("categoryName", category.getName());
                    try {
                        java.util.Set<com.basketball.app.model.User> coaches = category.getCoaches();
                        if (coaches != null && !coaches.isEmpty()) {
                            String coachName = coaches.stream()
                                .findFirst()
                                .map(com.basketball.app.model.User::getName)
                                .orElse(null);
                            if (coachName != null) eventMap.put("coachName", coachName);
                        }
                    } catch (Exception e) {
                        System.err.println("Error loading coaches for category " + category.getId() + ": " + e.getMessage());
                    }
                });
            } catch (Exception e) {
                System.err.println("Error loading category for event " + event.getId() + ": " + e.getMessage());
            }
        }
        
        eventMap.put("opponent", event.getOpponent());
        eventMap.put("notes", event.getNotes());
        eventMap.put("isHomeMatch", event.getIsHomeMatch());
        eventMap.put("matchWon", event.getMatchWon());
        eventMap.put("ourScore", event.getOurScore());
        eventMap.put("opponentScore", event.getOpponentScore());
        eventMap.put("cancelled", Boolean.TRUE.equals(event.getCancelled()));
        eventMap.put("createdAt", event.getCreatedAt());
        eventMap.put("updatedAt", event.getUpdatedAt());
        return eventMap;
    }
    
}

