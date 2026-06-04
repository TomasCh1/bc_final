package com.basketball.app.service;

import com.basketball.app.dto.EventLiveActionCreateRequest;
import com.basketball.app.model.Event;
import com.basketball.app.model.EventLiveAction;
import com.basketball.app.model.EventOpponentPlayer;
import com.basketball.app.repository.EventLiveActionRepository;
import com.basketball.app.repository.EventOpponentPlayerRepository;
import com.basketball.app.repository.EventRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@SuppressWarnings("null")
public class EventLiveActionService {

    private static final int MAX_ON_COURT = 5;

    private final EventRepository eventRepository;
    private final EventLiveActionRepository eventLiveActionRepository;
    private final EventOpponentPlayerRepository eventOpponentPlayerRepository;
    private final ObjectMapper objectMapper;

    public EventLiveActionService(
            EventRepository eventRepository,
            EventLiveActionRepository eventLiveActionRepository,
            EventOpponentPlayerRepository eventOpponentPlayerRepository,
            ObjectMapper objectMapper
    ) {
        this.eventRepository = eventRepository;
        this.eventLiveActionRepository = eventLiveActionRepository;
        this.eventOpponentPlayerRepository = eventOpponentPlayerRepository;
        this.objectMapper = objectMapper;
    }

    public List<Map<String, Object>> getLiveActions(Long eventId) {
        Event event = requireMatchEvent(eventId);
        return eventLiveActionRepository.findByEventIdOrderByCreatedAtAscIdAsc(event.getId()).stream()
                .map(this::toLiveActionMap)
                .collect(Collectors.toList());
    }

    @Transactional
    public Map<String, Object> createLiveAction(Long eventId, EventLiveActionCreateRequest request) {
        Event event = requireMatchEvent(eventId);
        validateCreateRequest(request);

        EventLiveAction action = new EventLiveAction();
        action.setEventId(event.getId());
        action.setPeriod(request.getPeriod());
        action.setClockSeconds(request.getClockSeconds());
        action.setTeamSide(request.getTeamSide());
        action.setActionType(request.getActionType());
        action.setOurPlayerId(request.getOurPlayerId());
        action.setOpponentJerseyNumber(request.getOpponentJerseyNumber());
        action.setPoints(request.getPoints());
        action.setOnCourtPlayerIdsJson(serializeOnCourtIds(request.getOnCourtPlayerIds()));

        EventLiveAction saved = Objects.requireNonNull(eventLiveActionRepository.save(action));
        saveOpponentToRosterIfPresent(event.getId(), saved.getTeamSide(), saved.getOpponentJerseyNumber());
        syncEventScoreFromActions(event);
        return toLiveActionMap(saved);
    }

    @Transactional
    public void deleteLiveAction(Long eventId, Long actionId) {
        Event event = requireMatchEvent(eventId);
        EventLiveAction action = eventLiveActionRepository.findByIdAndEventId(actionId, event.getId())
                .orElseThrow(() -> new RuntimeException("Live action not found: " + actionId));
        eventLiveActionRepository.delete(action);
        syncEventScoreFromActions(event);
    }

    public Map<String, Object> getLiveSummary(Long eventId) {
        Event event = requireMatchEvent(eventId);
        List<EventLiveAction> actions = eventLiveActionRepository.findByEventIdOrderByCreatedAtAscIdAsc(event.getId());
        Summary summary = recompute(actions);

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("eventId", event.getId());
        out.put("ourScore", summary.ourScore());
        out.put("opponentScore", summary.opponentScore());
        out.put("scoreDiff", summary.ourScore() - summary.opponentScore());
        out.put("matchWon", summary.matchWon());
        out.put("vp", summary.matchWon() == null ? "-" : (summary.matchWon() ? "V" : "P"));
        out.put("teamPlusMinus", summary.ourScore() - summary.opponentScore());
        out.put("playerPlusMinus", summary.playerPlusMinus());
        out.put("opponentByJersey", summary.opponentByJersey().entrySet().stream()
                .sorted(Comparator.comparingInt(Map.Entry::getKey))
                .map(e -> Map.<String, Object>of("jerseyNumber", e.getKey(), "points", e.getValue()))
                .collect(Collectors.toList()));
        out.put("opponentFoulsByJersey", summary.opponentFoulsByJersey().entrySet().stream()
                .sorted(Comparator.comparingInt(Map.Entry::getKey))
                .map(e -> Map.<String, Object>of("jerseyNumber", e.getKey(), "fouls", e.getValue()))
                .collect(Collectors.toList()));
        out.put("onCourtPlayerIds", summary.onCourtPlayerIds());
        out.put("actionsCount", actions.size());
        return out;
    }

    private void validateCreateRequest(EventLiveActionCreateRequest request) {
        if (request == null) {
            throw new RuntimeException("Request is required.");
        }

        EventLiveAction.ActionType actionType = request.getActionType();
        EventLiveAction.TeamSide teamSide = request.getTeamSide();
        Integer points = request.getPoints();

        if (actionType == EventLiveAction.ActionType.SCORE) {
            if (points == null || points <= 0 || points > 3) {
                throw new RuntimeException("Score action must include points from 1 to 3.");
            }
            if (teamSide == EventLiveAction.TeamSide.OPPONENT && request.getOpponentJerseyNumber() == null) {
                throw new RuntimeException("Opponent score requires opponent jersey number.");
            }
        }

        if (actionType == EventLiveAction.ActionType.FOUL) {
            if (teamSide == EventLiveAction.TeamSide.OUR && request.getOurPlayerId() == null) {
                throw new RuntimeException("OUR foul requires our player ID.");
            }
            if (teamSide == EventLiveAction.TeamSide.OPPONENT && request.getOpponentJerseyNumber() == null) {
                throw new RuntimeException("OPPONENT foul requires opponent jersey number.");
            }
            if (points != null && points != 0) {
                throw new RuntimeException("Foul action cannot carry non-zero points.");
            }
        }

        if (actionType == EventLiveAction.ActionType.SUB_IN || actionType == EventLiveAction.ActionType.SUB_OUT) {
            if (teamSide == EventLiveAction.TeamSide.OUR && request.getOurPlayerId() == null) {
                throw new RuntimeException("OUR substitution requires our player ID.");
            }
            if (teamSide == EventLiveAction.TeamSide.OPPONENT && request.getOpponentJerseyNumber() == null) {
                throw new RuntimeException("OPPONENT substitution requires opponent jersey number.");
            }
            if (teamSide == null) {
                throw new RuntimeException("Substitution action requires team side.");
            }
            if (points != null && points != 0) {
                throw new RuntimeException("Substitution action cannot carry non-zero points.");
            }
        }
    }

    private Event requireMatchEvent(Long eventId) {
        if (eventId == null) {
            throw new RuntimeException("Event ID is required.");
        }
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new RuntimeException("Event not found: " + eventId));
        if (event.getType() != Event.EventType.MATCH) {
            throw new RuntimeException("Live actions are allowed only for MATCH events.");
        }
        return event;
    }

    private String serializeOnCourtIds(List<Long> onCourtPlayerIds) {
        if (onCourtPlayerIds == null) return null;
        List<Long> normalized = normalizeOnCourtIds(onCourtPlayerIds);
        try {
            return objectMapper.writeValueAsString(normalized);
        } catch (Exception e) {
            throw new RuntimeException("Could not serialize on-court player IDs.");
        }
    }

    private List<Long> parseOnCourtIds(String json) {
        if (json == null || json.isBlank()) return List.of();
        try {
            List<Long> ids = objectMapper.readValue(json, new TypeReference<List<Long>>() {});
            return normalizeOnCourtIds(ids);
        } catch (Exception e) {
            return List.of();
        }
    }

    private List<Long> normalizeOnCourtIds(List<Long> ids) {
        if (ids == null || ids.isEmpty()) return List.of();
        LinkedHashSet<Long> out = new LinkedHashSet<>();
        for (Long id : ids) {
            if (id != null && id > 0) {
                out.add(id);
            }
            if (out.size() >= MAX_ON_COURT) break;
        }
        return new ArrayList<>(out);
    }

    private Summary recompute(List<EventLiveAction> actions) {
        int ourScore = 0;
        int opponentScore = 0;
        Map<Long, Integer> playerPlusMinus = new LinkedHashMap<>();
        Map<Integer, Integer> opponentByJersey = new LinkedHashMap<>();
        Map<Integer, Integer> opponentFoulsByJersey = new LinkedHashMap<>();
        LinkedHashSet<Long> lineup = new LinkedHashSet<>();

        for (EventLiveAction action : actions) {
            List<Long> snapshot = parseOnCourtIds(action.getOnCourtPlayerIdsJson());
            if (!snapshot.isEmpty()) {
                lineup = new LinkedHashSet<>(snapshot);
            }

            if (action.getActionType() == EventLiveAction.ActionType.SUB_IN && action.getOurPlayerId() != null) {
                if (!lineup.contains(action.getOurPlayerId()) && lineup.size() < MAX_ON_COURT) {
                    lineup.add(action.getOurPlayerId());
                }
            } else if (action.getActionType() == EventLiveAction.ActionType.SUB_OUT && action.getOurPlayerId() != null) {
                lineup.remove(action.getOurPlayerId());
            } else if (action.getActionType() == EventLiveAction.ActionType.SCORE) {
                int points = action.getPoints() != null ? action.getPoints() : 0;
                List<Long> activeLineup = snapshot.isEmpty() ? new ArrayList<>(lineup) : snapshot;
                if (action.getTeamSide() == EventLiveAction.TeamSide.OUR) {
                    ourScore += points;
                    for (Long playerId : activeLineup) {
                        playerPlusMinus.put(playerId, playerPlusMinus.getOrDefault(playerId, 0) + points);
                    }
                } else {
                    opponentScore += points;
                    for (Long playerId : activeLineup) {
                        playerPlusMinus.put(playerId, playerPlusMinus.getOrDefault(playerId, 0) - points);
                    }
                    if (action.getOpponentJerseyNumber() != null) {
                        Integer jersey = action.getOpponentJerseyNumber();
                        opponentByJersey.put(jersey, opponentByJersey.getOrDefault(jersey, 0) + points);
                    }
                }
            } else if (action.getActionType() == EventLiveAction.ActionType.FOUL) {
                if (action.getTeamSide() == EventLiveAction.TeamSide.OPPONENT && action.getOpponentJerseyNumber() != null) {
                    Integer jersey = action.getOpponentJerseyNumber();
                    opponentFoulsByJersey.put(jersey, opponentFoulsByJersey.getOrDefault(jersey, 0) + 1);
                }
            }
        }

        Boolean matchWon = null;
        if (ourScore > opponentScore) {
            matchWon = true;
        } else if (ourScore < opponentScore) {
            matchWon = false;
        }

        return new Summary(
                ourScore,
                opponentScore,
                matchWon,
                playerPlusMinus,
                opponentByJersey,
                opponentFoulsByJersey,
                new ArrayList<>(lineup)
        );
    }

    private void saveOpponentToRosterIfPresent(Long eventId, EventLiveAction.TeamSide teamSide, Integer jerseyNumber) {
        if (eventId == null || teamSide != EventLiveAction.TeamSide.OPPONENT || jerseyNumber == null || jerseyNumber <= 0) {
            return;
        }
        if (eventOpponentPlayerRepository.existsByEventIdAndJerseyNumber(eventId, jerseyNumber)) {
            return;
        }
        EventOpponentPlayer row = new EventOpponentPlayer();
        row.setEventId(eventId);
        row.setJerseyNumber(jerseyNumber);
        eventOpponentPlayerRepository.save(row);
    }

    private void syncEventScoreFromActions(Event event) {
        List<EventLiveAction> actions = eventLiveActionRepository.findByEventIdOrderByCreatedAtAscIdAsc(event.getId());
        Summary summary = recompute(actions);
        event.setOurScore(summary.ourScore());
        event.setOpponentScore(summary.opponentScore());
        event.setMatchWon(summary.matchWon());
        eventRepository.save(event);
    }

    private Map<String, Object> toLiveActionMap(EventLiveAction action) {
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("id", action.getId());
        out.put("eventId", action.getEventId());
        out.put("period", action.getPeriod());
        out.put("clockSeconds", action.getClockSeconds());
        out.put("teamSide", action.getTeamSide());
        out.put("actionType", action.getActionType());
        out.put("ourPlayerId", action.getOurPlayerId());
        out.put("opponentJerseyNumber", action.getOpponentJerseyNumber());
        out.put("points", action.getPoints());
        out.put("onCourtPlayerIds", parseOnCourtIds(action.getOnCourtPlayerIdsJson()));
        out.put("createdAt", action.getCreatedAt());
        out.put("createdBy", action.getCreatedBy());
        return out;
    }

    private record Summary(
            int ourScore,
            int opponentScore,
            Boolean matchWon,
            Map<Long, Integer> playerPlusMinus,
            Map<Integer, Integer> opponentByJersey,
            Map<Integer, Integer> opponentFoulsByJersey,
            List<Long> onCourtPlayerIds
    ) {
        private Summary {
            Objects.requireNonNull(playerPlusMinus);
            Objects.requireNonNull(opponentByJersey);
            Objects.requireNonNull(opponentFoulsByJersey);
            Objects.requireNonNull(onCourtPlayerIds);
        }
    }
}
