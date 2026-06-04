package com.basketball.app.service;

import com.basketball.app.dto.EventNominationItemRequest;
import com.basketball.app.model.Event;
import com.basketball.app.model.EventNominatedPlayer;
import com.basketball.app.model.User;
import com.basketball.app.repository.EventNominatedPlayerRepository;
import com.basketball.app.repository.EventRepository;
import com.basketball.app.repository.UserRepository;
import com.basketball.app.util.UserNames;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class EventNominationService {

    private final EventRepository eventRepository;
    private final UserRepository userRepository;
    private final EventNominatedPlayerRepository eventNominatedPlayerRepository;

    public EventNominationService(
            EventRepository eventRepository,
            UserRepository userRepository,
            EventNominatedPlayerRepository eventNominatedPlayerRepository
    ) {
        this.eventRepository = eventRepository;
        this.userRepository = userRepository;
        this.eventNominatedPlayerRepository = eventNominatedPlayerRepository;
    }

    public Map<String, Object> getNominationsForEvent(Long eventId) {
        Event event = requireMatchEvent(eventId);
        List<EventNominatedPlayer> rows = eventNominatedPlayerRepository.findByEventId(event.getId());
        List<Long> playerIds = rows.stream()
                .map(EventNominatedPlayer::getPlayerId)
                .distinct()
                .collect(Collectors.toList());
        Set<Long> resolvablePlayerIds = playerIds.stream()
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<Long, String> playerNameById = userRepository.findAllById(resolvablePlayerIds).stream()
                .collect(Collectors.toMap(User::getId, UserNames::getDisplayName));
        List<Map<String, Object>> nominations = rows.stream()
                .map(row -> Map.<String, Object>of(
                        "playerId", row.getPlayerId(),
                        "jerseyNumber", row.getJerseyNumber() == null ? "" : row.getJerseyNumber(),
                        "isStartingFive", Boolean.TRUE.equals(row.getStartingFive()),
                        "playerName", playerNameById.getOrDefault(row.getPlayerId(), "")
                ))
                .collect(Collectors.toList());
        return Map.of(
                "eventId", event.getId(),
                "playerIds", playerIds,
                "nominations", nominations
        );
    }

    @Transactional
    public Map<String, Object> replaceNominationsForEvent(
            Long eventId,
            List<Long> requestedPlayerIds,
            List<EventNominationItemRequest> requestedNominations
    ) {
        Event event = requireMatchEvent(eventId);
        List<NormalizedNomination> normalizedNominations = normalizeNominations(requestedPlayerIds, requestedNominations);
        List<Long> playerIds = normalizedNominations.stream()
                .map(NormalizedNomination::playerId)
                .collect(Collectors.toList());

        if (!playerIds.isEmpty()) {
            List<User> players = userRepository.findAllById(playerIds);
            if (players.size() != playerIds.size()) {
                throw new RuntimeException("Some nominated players were not found.");
            }
            for (User user : players) {
                if (user.getRole() != User.Role.PLAYER) {
                    throw new RuntimeException("Only users with role PLAYER can be nominated.");
                }
                if (Boolean.TRUE.equals(user.getDeleted())) {
                    throw new RuntimeException("Deleted players cannot be nominated.");
                }
            }
        }

        // Ensure old nominations are physically deleted before we insert replacements,
        // otherwise DB unique constraint (event_id, player_id) can fail in one transaction.
        eventNominatedPlayerRepository.deleteByEventId(event.getId());
        eventNominatedPlayerRepository.flush();

        if (!normalizedNominations.isEmpty()) {
            List<EventNominatedPlayer> rows = new ArrayList<>();
            for (NormalizedNomination nomination : normalizedNominations) {
                EventNominatedPlayer row = new EventNominatedPlayer();
                row.setEventId(event.getId());
                row.setPlayerId(nomination.playerId());
                row.setJerseyNumber(nomination.jerseyNumber());
                row.setStartingFive(nomination.startingFive());
                rows.add(row);
            }
            eventNominatedPlayerRepository.saveAll(rows);
            eventNominatedPlayerRepository.flush();
        }

        Set<Long> resolvableSavedPlayerIds = playerIds.stream()
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<Long, String> savedPlayerNameById = userRepository.findAllById(resolvableSavedPlayerIds).stream()
                .collect(Collectors.toMap(User::getId, UserNames::getDisplayName));
        List<Map<String, Object>> nominations = normalizedNominations.stream()
                .map(item -> Map.<String, Object>of(
                        "playerId", item.playerId(),
                        "jerseyNumber", item.jerseyNumber(),
                        "isStartingFive", item.startingFive(),
                        "playerName", savedPlayerNameById.getOrDefault(item.playerId(), "")
                ))
                .collect(Collectors.toList());

        return Map.of(
                "eventId", event.getId(),
                "playerIds", playerIds,
                "nominations", nominations
        );
    }

    private Event requireMatchEvent(Long eventId) {
        if (eventId == null) {
            throw new RuntimeException("Event ID is required.");
        }
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new RuntimeException("Event not found: " + eventId));
        if (event.getType() != Event.EventType.MATCH) {
            throw new RuntimeException("Nominations are allowed only for MATCH events.");
        }
        return event;
    }

    private List<NormalizedNomination> normalizeNominations(
            List<Long> requestedPlayerIds,
            List<EventNominationItemRequest> requestedNominations
    ) {
        if ((requestedNominations == null || requestedNominations.isEmpty())
                && (requestedPlayerIds == null || requestedPlayerIds.isEmpty())) {
            return List.of();
        }

        LinkedHashMap<Long, NormalizedNomination> normalizedByPlayer = new LinkedHashMap<>();
        if (requestedNominations != null && !requestedNominations.isEmpty()) {
            for (EventNominationItemRequest nomination : requestedNominations) {
                if (nomination == null) continue;
                Long playerId = nomination.getPlayerId();
                Integer jerseyNumber = nomination.getJerseyNumber();
                boolean startingFive = Boolean.TRUE.equals(nomination.getStartingFive());
                if (playerId == null || playerId <= 0) continue;
                if (jerseyNumber == null || jerseyNumber <= 0) {
                    throw new RuntimeException("Every nominated player must have a valid jersey number.");
                }
                normalizedByPlayer.put(playerId, new NormalizedNomination(playerId, jerseyNumber, startingFive));
            }
        } else {
            // Backward compatibility: convert playerIds into placeholder jersey numbers.
            int fallbackJersey = 1;
            Set<Long> uniqueIds = new LinkedHashSet<>();
            for (Long playerId : requestedPlayerIds) {
                if (playerId == null || playerId <= 0) continue;
                uniqueIds.add(playerId);
            }
            for (Long playerId : uniqueIds) {
                while (containsJersey(normalizedByPlayer, fallbackJersey)) {
                    fallbackJersey++;
                }
                normalizedByPlayer.put(playerId, new NormalizedNomination(playerId, fallbackJersey++, false));
            }
        }

        Set<Integer> seenJerseyNumbers = new LinkedHashSet<>();
        int startingFiveCount = 0;
        for (NormalizedNomination nomination : normalizedByPlayer.values()) {
            Integer jersey = nomination.jerseyNumber();
            if (!seenJerseyNumbers.add(jersey)) {
                throw new RuntimeException("Two nominated players cannot share the same jersey number.");
            }
            if (nomination.startingFive()) {
                startingFiveCount++;
            }
        }
        if (startingFiveCount > 5) {
            throw new RuntimeException("You can select at most 5 players in the starting five.");
        }

        return new ArrayList<>(normalizedByPlayer.values());
    }

    private boolean containsJersey(Map<Long, NormalizedNomination> normalizedByPlayer, int jersey) {
        return normalizedByPlayer.values().stream()
                .anyMatch(item -> Objects.equals(item.jerseyNumber(), jersey));
    }

    private record NormalizedNomination(Long playerId, Integer jerseyNumber, boolean startingFive) {
    }
}
