package com.basketball.app.service;

import com.basketball.app.dto.EventRefereeItemRequest;
import com.basketball.app.model.Event;
import com.basketball.app.model.EventReferee;
import com.basketball.app.repository.EventRefereeRepository;
import com.basketball.app.repository.EventRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class EventRefereeService {

    private static final int MAX_REFEREES = 3;

    private final EventRepository eventRepository;
    private final EventRefereeRepository eventRefereeRepository;

    public EventRefereeService(EventRepository eventRepository, EventRefereeRepository eventRefereeRepository) {
        this.eventRepository = eventRepository;
        this.eventRefereeRepository = eventRefereeRepository;
    }

    public Map<String, Object> getRefereesForEvent(Long eventId) {
        Event event = requireMatchEvent(eventId);
        List<EventReferee> rows = eventRefereeRepository.findByEventIdOrderBySortOrderAsc(event.getId());
        List<Map<String, Object>> referees = rows.stream()
                .map(row -> Map.<String, Object>of(
                        "name", row.getName() != null ? row.getName() : "",
                        "grade", row.getGrade() != null ? row.getGrade() : ""
                ))
                .collect(Collectors.toList());
        return Map.of(
                "eventId", event.getId(),
                "referees", referees
        );
    }

    @Transactional
    public Map<String, Object> replaceRefereesForEvent(Long eventId, List<EventRefereeItemRequest> requested) {
        Event event = requireMatchEvent(eventId);
        List<NormalizedRef> normalized = normalizeReferees(requested);

        eventRefereeRepository.deleteByEventId(event.getId());
        eventRefereeRepository.flush();

        if (!normalized.isEmpty()) {
            List<EventReferee> rows = new ArrayList<>();
            for (int i = 0; i < normalized.size(); i++) {
                NormalizedRef ref = normalized.get(i);
                EventReferee row = new EventReferee();
                row.setEventId(event.getId());
                row.setSortOrder(i);
                row.setName(ref.name());
                row.setGrade(ref.grade());
                rows.add(row);
            }
            eventRefereeRepository.saveAll(rows);
            eventRefereeRepository.flush();
        }

        List<Map<String, Object>> out = normalized.stream()
                .map(ref -> Map.<String, Object>of(
                        "name", ref.name(),
                        "grade", ref.grade() != null ? ref.grade() : ""
                ))
                .collect(Collectors.toList());

        return Map.of(
                "eventId", event.getId(),
                "referees", out
        );
    }

    private Event requireMatchEvent(Long eventId) {
        if (eventId == null) {
            throw new RuntimeException("Event ID is required.");
        }
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new RuntimeException("Event not found: " + eventId));
        if (event.getType() != Event.EventType.MATCH) {
            throw new RuntimeException("Referees are allowed only for MATCH events.");
        }
        return event;
    }

    private List<NormalizedRef> normalizeReferees(List<EventRefereeItemRequest> requested) {
        if (requested == null || requested.isEmpty()) {
            return List.of();
        }
        List<NormalizedRef> out = new ArrayList<>();
        for (EventRefereeItemRequest item : requested) {
            if (item == null) {
                continue;
            }
            String name = item.getName() != null ? item.getName().trim() : "";
            if (name.isEmpty()) {
                continue;
            }
            if (name.length() > 255) {
                throw new RuntimeException("Referee name must be at most 255 characters.");
            }
            String grade = item.getGrade() != null ? item.getGrade().trim() : "";
            if (grade.length() > 64) {
                throw new RuntimeException("Referee grade must be at most 64 characters.");
            }
            String gradeOrNull = grade.isEmpty() ? null : grade;
            out.add(new NormalizedRef(name, gradeOrNull));
            if (out.size() > MAX_REFEREES) {
                throw new RuntimeException("You can add at most " + MAX_REFEREES + " referees per game.");
            }
        }
        return out;
    }

    private record NormalizedRef(String name, String grade) {
    }
}
