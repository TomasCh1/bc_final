package com.basketball.app.service;

import com.basketball.app.model.Event;
import com.basketball.app.model.EventLiveAction;
import com.basketball.app.model.EventOpponentPlayer;
import com.basketball.app.repository.EventLiveActionRepository;
import com.basketball.app.repository.EventOpponentPlayerRepository;
import com.basketball.app.repository.EventRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class EventOpponentService {

    private static final int MAX_OPPONENTS = 12;

    private final EventRepository eventRepository;
    private final EventOpponentPlayerRepository eventOpponentPlayerRepository;
    private final EventLiveActionRepository eventLiveActionRepository;

    public EventOpponentService(EventRepository eventRepository,
                                EventOpponentPlayerRepository eventOpponentPlayerRepository,
                                EventLiveActionRepository eventLiveActionRepository) {
        this.eventRepository = eventRepository;
        this.eventOpponentPlayerRepository = eventOpponentPlayerRepository;
        this.eventLiveActionRepository = eventLiveActionRepository;
    }

    public Map<String, Object> getOpponentsForEvent(Long eventId) {
        Event event = requireMatchEvent(eventId);
        List<EventOpponentPlayer> rows = eventOpponentPlayerRepository.findByEventIdOrderByJerseyNumberAsc(event.getId());
        Set<Integer> unique = new LinkedHashSet<>(rows.stream()
                .map(EventOpponentPlayer::getJerseyNumber)
                .collect(Collectors.toList()));

        eventLiveActionRepository.findByEventIdOrderByCreatedAtAscIdAsc(event.getId()).forEach(action -> {
            Integer jersey = action.getOpponentJerseyNumber();
            if (jersey != null && jersey > 0) {
                unique.add(jersey);
            }
        });

        List<Integer> jerseys = unique.stream().sorted().collect(Collectors.toList());
        return Map.of(
                "eventId", event.getId(),
                "jerseyNumbers", jerseys
        );
    }

    @Transactional
    public Map<String, Object> replaceOpponentsForEvent(Long eventId, List<Integer> jerseyNumbers) {
        Event event = requireMatchEvent(eventId);
        List<Integer> normalized = normalizeJerseys(jerseyNumbers);
        Set<Integer> want = new LinkedHashSet<>(normalized);

        List<EventOpponentPlayer> existing = eventOpponentPlayerRepository.findByEventIdOrderByJerseyNumberAsc(event.getId());
        List<Integer> removedJerseys = new ArrayList<>();
        for (EventOpponentPlayer row : new ArrayList<>(existing)) {
            if (!want.contains(row.getJerseyNumber())) {
                removedJerseys.add(row.getJerseyNumber());
            }
        }

        if (!removedJerseys.isEmpty()) {
            Set<Integer> locked = eventLiveActionRepository.findByEventIdAndOpponentJerseyNumberIn(event.getId(), removedJerseys)
                    .stream()
                    .map(EventLiveAction::getOpponentJerseyNumber)
                    .filter(j -> j != null && j > 0)
                    .collect(Collectors.toCollection(LinkedHashSet::new));
            if (!locked.isEmpty()) {
                String blocked = locked.stream()
                        .sorted()
                        .map(j -> "#" + j)
                        .collect(Collectors.joining(", "));
                throw new RuntimeException(
                        "Cannot update/delete opponents already used on court: " + blocked + "."
                );
            }
            for (EventOpponentPlayer row : existing) {
                if (removedJerseys.contains(row.getJerseyNumber())) {
                    eventOpponentPlayerRepository.delete(row);
                }
            }
            eventOpponentPlayerRepository.flush();
        }

        for (Integer jersey : want) {
            if (!eventOpponentPlayerRepository.existsByEventIdAndJerseyNumber(event.getId(), jersey)) {
                EventOpponentPlayer row = new EventOpponentPlayer();
                row.setEventId(event.getId());
                row.setJerseyNumber(jersey);
                eventOpponentPlayerRepository.save(row);
            }
        }
        return getOpponentsForEvent(event.getId());
    }

    private Event requireMatchEvent(Long eventId) {
        if (eventId == null) {
            throw new RuntimeException("Event ID is required.");
        }
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new RuntimeException("Event not found: " + eventId));
        if (event.getType() != Event.EventType.MATCH) {
            throw new RuntimeException("Opponents can be set only for MATCH events.");
        }
        return event;
    }

    private List<Integer> normalizeJerseys(List<Integer> jerseyNumbers) {
        if (jerseyNumbers == null || jerseyNumbers.isEmpty()) {
            return List.of();
        }
        Set<Integer> unique = new LinkedHashSet<>();
        for (Integer jersey : jerseyNumbers) {
            if (jersey == null) continue;
            if (jersey <= 0) {
                throw new RuntimeException("Opponent jersey numbers must be positive.");
            }
            unique.add(jersey);
            if (unique.size() > MAX_OPPONENTS) {
                throw new RuntimeException("Maximum " + MAX_OPPONENTS + " opponent jersey numbers are allowed.");
            }
        }
        return unique.stream().sorted().collect(Collectors.toList());
    }

}
