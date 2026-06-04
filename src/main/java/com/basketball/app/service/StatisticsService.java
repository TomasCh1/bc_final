package com.basketball.app.service;

import com.basketball.app.dto.StatisticsCreateRequest;
import com.basketball.app.dto.StatisticsUpdateRequest;
import com.basketball.app.model.Statistics;
import com.basketball.app.model.Event;
import com.basketball.app.model.User;
import com.basketball.app.repository.StatisticsRepository;
import com.basketball.app.repository.EventRepository;
import com.basketball.app.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class StatisticsService {

    private final StatisticsRepository statisticsRepository;
    private final EventRepository eventRepository;
    private final UserRepository userRepository;

    public StatisticsService(StatisticsRepository statisticsRepository,
                           EventRepository eventRepository,
                           UserRepository userRepository) {
        this.statisticsRepository = statisticsRepository;
        this.eventRepository = eventRepository;
        this.userRepository = userRepository;
    }

    /** Apply map keys (MIN, 2PÚ, PTS, etc.) to entity columns and set statIndex from formula. */
    private void applyValuesToStatistics(Statistics stat, Map<String, Object> values) {
        if (values == null) return;
        stat.setMin(getDoubleValue(values, "MIN", null));
        stat.setTwoPMade(getIntValue(values, "2PÚ"));
        stat.setTwoPAttempts(getIntValue(values, "2PC"));
        stat.setThreePMade(getIntValue(values, "3PÚ"));
        stat.setThreePAttempts(getIntValue(values, "3PC"));
        stat.setFgMade(getIntValue(values, "FGÚ"));
        stat.setFgAttempts(getIntValue(values, "FGC"));
        stat.setFtMade(getIntValue(values, "FTÚ"));
        stat.setFtAttempts(getIntValue(values, "FTC"));
        stat.setPts(getIntValue(values, "PTS"));
        stat.setFoulsPlus(getIntValue(values, "F+"));
        stat.setFoulsMinus(getIntValue(values, "F-"));
        stat.setOffReb(getIntValue(values, "OFF"));
        stat.setDefReb(getIntValue(values, "DEF"));
        stat.setStl(getIntValue(values, "STL"));
        stat.setAst(getIntValue(values, "AST"));
        stat.setBlk(getIntValue(values, "BLK"));
        stat.setTurnovers(getIntValue(values, "TO"));
        Double index = values.containsKey("INDEX") && values.get("INDEX") != null
            ? getDoubleValue(values, "INDEX", 0.0) : calculateIndex(values);
        stat.setStatIndex(index);
    }

    private static Integer getIntValue(Map<String, Object> values, String key) {
        Object v = values.get(key);
        if (v == null) return null;
        if (v instanceof Number) return ((Number) v).intValue();
        if (v instanceof String) {
            try { return Integer.parseInt((String) v); } catch (NumberFormatException e) { return null; }
        }
        return null;
    }

    /**
     * Calculate INDEX formula:
     * (PTS + OFF + DEF + AST + STL + BLK) - ((FGC - FGÚ) + (FTC - FTÚ) + TO)
     */
    public Double calculateIndex(Map<String, Object> values) {
        try {
            // Get values with defaults
            double pts = getDoubleValue(values, "PTS", 0.0);
            double off = getDoubleValue(values, "OFF", 0.0);
            double def = getDoubleValue(values, "DEF", 0.0);
            double ast = getDoubleValue(values, "AST", 0.0);
            double stl = getDoubleValue(values, "STL", 0.0);
            double blk = getDoubleValue(values, "BLK", 0.0);
            double fgc = getDoubleValue(values, "FGC", 0.0);
            double fgu = getDoubleValue(values, "FGÚ", 0.0);
            double ftc = getDoubleValue(values, "FTC", 0.0);
            double ftu = getDoubleValue(values, "FTÚ", 0.0);
            double to = getDoubleValue(values, "TO", 0.0);

            // Calculate INDEX
            double index = (pts + off + def + ast + stl + blk) - ((fgc - fgu) + (ftc - ftu) + to);
            return Math.round(index * 100.0) / 100.0; // Round to 2 decimal places
        } catch (Exception e) {
            throw new RuntimeException("Error calculating INDEX: " + e.getMessage(), e);
        }
    }

    private Double getDoubleValue(Map<String, Object> values, String key, Double defaultValue) {
        Object value = values.get(key);
        if (value == null) {
            return defaultValue;
        }
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }
        if (value instanceof String) {
            try {
                return Double.parseDouble((String) value);
            } catch (NumberFormatException e) {
                return defaultValue;
            }
        }
        return defaultValue;
    }

    /**
     * Create statistics record
     */
    @Transactional
    public Statistics createStatistics(StatisticsCreateRequest request) {
        // Validate event exists
        eventRepository.findById(request.getEventId())
                .orElseThrow(() -> new RuntimeException("Event not found: " + request.getEventId()));

        // Validate player exists and is not deleted
        var player = userRepository.findById(request.getPlayerId())
                .orElseThrow(() -> new RuntimeException("Player not found: " + request.getPlayerId()));
        if (Boolean.TRUE.equals(player.getDeleted())) {
            throw new RuntimeException("Player not found: " + request.getPlayerId());
        }

        // Opponent is validated by @NotBlank on request

        Statistics statistics = new Statistics();
        statistics.setEventId(request.getEventId());
        statistics.setPlayerId(request.getPlayerId());
        statistics.setStatType(Statistics.StatType.GAME);
        statistics.setOpponent(request.getOpponent());
        statistics.setTimestamp(LocalDateTime.now());
        applyValuesToStatistics(statistics, request.getValues());

        return statisticsRepository.save(statistics);
    }

    /**
     * Update statistics record
     */
    @Transactional
    public Statistics updateStatistics(Long id, StatisticsUpdateRequest request) {
        Statistics existing = statisticsRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Statistics not found: " + id));

        if (request.getOpponent() != null && !request.getOpponent().isBlank()) {
            existing.setOpponent(request.getOpponent());
        }
        if (request.getValues() != null) {
            applyValuesToStatistics(existing, request.getValues());
        }

        return statisticsRepository.save(existing);
    }

    /**
     * Get statistics by ID
     */
    public Optional<Statistics> findById(Long id) {
        return statisticsRepository.findById(id);
    }

    /**
     * Get all statistics
     */
    public List<Statistics> findAll() {
        return statisticsRepository.findAll();
    }

    /**
     * Get statistics by event
     */
    public List<Statistics> findByEvent(Long eventId) {
        return statisticsRepository.findByEventId(eventId);
    }

    /**
     * Get statistics by player
     */
    public List<Statistics> findByPlayer(Long playerId) {
        return statisticsRepository.findByPlayerId(playerId);
    }

    /**
     * Get statistics by stat type
     */
    public List<Statistics> findByStatType(Statistics.StatType statType) {
        return statisticsRepository.findByStatType(statType);
    }

    /**
     * Get statistics with filters
     */
    public List<Statistics> findWithFilters(Long playerId, Long eventId, Long categoryId,
                                          Statistics.StatType statType, LocalDate startDate,
                                          LocalDate endDate, String opponent) {
        List<Statistics> results;

        if (playerId != null) {
            results = statisticsRepository.findByPlayerId(playerId);
        } else if (eventId != null) {
            results = statisticsRepository.findByEventId(eventId);
        } else if (categoryId != null && startDate == null && endDate == null) {
            results = statisticsRepository.findByCategoryId(categoryId);
        } else {
            results = statisticsRepository.findAll();
        }

        // Apply additional filters
        if (statType != null) {
            results = results.stream()
                    .filter(s -> s.getStatType() == statType)
                    .collect(Collectors.toList());
        }

        if (opponent != null && !opponent.trim().isEmpty()) {
            results = results.stream()
                    .filter(s -> s.getOpponent() != null && s.getOpponent().equalsIgnoreCase(opponent))
                    .collect(Collectors.toList());
        }

        if (startDate != null || endDate != null) {
            List<Long> eventIds = eventRepository.findAll().stream()
                    .filter(e -> {
                        if (startDate != null && e.getDate() != null && e.getDate().isBefore(startDate)) {
                            return false;
                        }
                        if (endDate != null && e.getDate() != null && e.getDate().isAfter(endDate)) {
                            return false;
                        }
                        if (categoryId != null && (e.getCategoryId() == null || !e.getCategoryId().equals(categoryId))) {
                            return false;
                        }
                        return true;
                    })
                    .map(e -> e.getId())
                    .collect(Collectors.toList());

            results = results.stream()
                    .filter(s -> eventIds.contains(s.getEventId()))
                    .collect(Collectors.toList());
        }

        return results;
    }

    /**
     * Get statistics with filters and return as maps with event/player.
     */
    public List<Map<String, Object>> findWithFiltersAsMaps(Long playerId, Long eventId, Long categoryId,
                                                           Statistics.StatType statType, LocalDate startDate,
                                                           LocalDate endDate, String opponent) {
        List<Statistics> list = findWithFilters(playerId, eventId, categoryId, statType, startDate, endDate, opponent);
        if (list.isEmpty()) {
            return Collections.emptyList();
        }
        Set<Long> eventIds = list.stream().map(Statistics::getEventId).filter(Objects::nonNull).collect(Collectors.toSet());
        Set<Long> playerIds = list.stream().map(Statistics::getPlayerId).filter(Objects::nonNull).collect(Collectors.toSet());
        Map<Long, Event> eventsById = new HashMap<>();
        Map<Long, User> usersById = new HashMap<>();
        if (!eventIds.isEmpty()) {
            eventRepository.findAllById(eventIds).forEach(e -> eventsById.put(e.getId(), e));
        }
        if (!playerIds.isEmpty()) {
            userRepository.findAllById(playerIds).forEach(u -> usersById.put(u.getId(), u));
        }
        return list.stream()
                .map(s -> toStatisticsMap(s, eventsById, usersById))
                .collect(Collectors.toList());
    }

    /**
     * Get statistics for the most recent N games.
     */
    public Map<String, Object> findRecentGameStatistics(Long categoryId, int limit, int offset) {
        Pageable pageable = PageRequest.of(offset / limit, limit);
        Page<Long> eventIdsPage = eventRepository.findRecentMatchEventIdsWithGameStats(categoryId, pageable);
        List<Long> eventIds = eventIdsPage.getContent();
        if (eventIds.isEmpty()) {
            Map<String, Object> out = new HashMap<>();
            out.put("statistics", Collections.emptyList());
            out.put("totalGames", 0L);
            out.put("hasMore", false);
            return out;
        }
        List<Statistics> statistics = statisticsRepository.findByEventIdIn(eventIds);
        Map<Long, Integer> order = new HashMap<>();
        for (int i = 0; i < eventIds.size(); i++) {
            order.put(eventIds.get(i), i);
        }
        statistics.sort(Comparator.comparing(s -> order.getOrDefault(s.getEventId(), 0)));
        Set<Long> eventIdsSet = new HashSet<>(eventIds);
        Set<Long> playerIds = statistics.stream().map(Statistics::getPlayerId).filter(Objects::nonNull).collect(Collectors.toSet());
        Map<Long, Event> eventsById = new HashMap<>();
        Map<Long, User> usersById = new HashMap<>();
        if (!eventIdsSet.isEmpty()) {
            eventRepository.findAllById(eventIdsSet).forEach(e -> eventsById.put(e.getId(), e));
        }
        if (!playerIds.isEmpty()) {
            userRepository.findAllById(playerIds).forEach(u -> usersById.put(u.getId(), u));
        }
        List<Map<String, Object>> maps = statistics.stream()
                .map(s -> toStatisticsMap(s, eventsById, usersById))
                .collect(Collectors.toList());
        Map<String, Object> out = new HashMap<>();
        out.put("statistics", maps);
        out.put("totalGames", eventIdsPage.getTotalElements());
        out.put("hasMore", (offset + limit) < eventIdsPage.getTotalElements());
        return out;
    }

    /**
     * Delete statistics
     */
    @Transactional
    public void deleteStatistics(Long id) {
        if (!statisticsRepository.existsById(id)) {
            throw new RuntimeException("Statistics not found: " + id);
        }
        statisticsRepository.deleteById(id);
    }

    private static Map<String, Object> buildValues(Statistics s) {
        Map<String, Object> m = new HashMap<>();
        if (s.getMin() != null) m.put("MIN", s.getMin());
        if (s.getTwoPMade() != null) m.put("2PÚ", s.getTwoPMade());
        if (s.getTwoPAttempts() != null) m.put("2PC", s.getTwoPAttempts());
        if (s.getThreePMade() != null) m.put("3PÚ", s.getThreePMade());
        if (s.getThreePAttempts() != null) m.put("3PC", s.getThreePAttempts());
        if (s.getFgMade() != null) m.put("FGÚ", s.getFgMade());
        if (s.getFgAttempts() != null) m.put("FGC", s.getFgAttempts());
        if (s.getFtMade() != null) m.put("FTÚ", s.getFtMade());
        if (s.getFtAttempts() != null) m.put("FTC", s.getFtAttempts());
        if (s.getPts() != null) m.put("PTS", s.getPts());
        if (s.getFoulsPlus() != null) m.put("F+", s.getFoulsPlus());
        if (s.getFoulsMinus() != null) m.put("F-", s.getFoulsMinus());
        if (s.getOffReb() != null) m.put("OFF", s.getOffReb());
        if (s.getDefReb() != null) m.put("DEF", s.getDefReb());
        if (s.getStl() != null) m.put("STL", s.getStl());
        if (s.getAst() != null) m.put("AST", s.getAst());
        if (s.getBlk() != null) m.put("BLK", s.getBlk());
        if (s.getTurnovers() != null) m.put("TO", s.getTurnovers());
        if (s.getStatIndex() != null) m.put("INDEX", s.getStatIndex());
        return m;
    }

    /**
     * Convert Statistics to map with enriched data
     */
    public Map<String, Object> toStatisticsMap(Statistics statistics) {
        return toStatisticsMap(statistics, null, null);
    }

    /**
     * Convert Statistics to map with enriched data
     */
    public Map<String, Object> toStatisticsMap(Statistics statistics,
                                                Map<Long, Event> eventsById,
                                                Map<Long, User> usersById) {
        Map<String, Object> statsMap = new HashMap<>();
        statsMap.put("id", statistics.getId());
        statsMap.put("eventId", statistics.getEventId());
        statsMap.put("playerId", statistics.getPlayerId());
        statsMap.put("statType", statistics.getStatType());
        statsMap.put("opponent", statistics.getOpponent());
        statsMap.put("timestamp", statistics.getTimestamp());
        statsMap.put("values", buildValues(statistics));

        User player = (usersById != null && statistics.getPlayerId() != null)
                ? usersById.get(statistics.getPlayerId())
                : null;
        if (player == null && statistics.getPlayerId() != null) {
            player = userRepository.findById(statistics.getPlayerId()).orElse(null);
        }
        if (player != null) {
            statsMap.put("playerName", com.basketball.app.util.UserNames.getDisplayName(player));
            statsMap.put("playerEmail", player.getEmail());
        }

        Event event = (eventsById != null && statistics.getEventId() != null)
                ? eventsById.get(statistics.getEventId())
                : null;
        if (event == null && statistics.getEventId() != null) {
            event = eventRepository.findById(statistics.getEventId()).orElse(null);
        }
        if (event != null) {
            statsMap.put("eventName", event.getName());
            statsMap.put("eventType", event.getType());
            statsMap.put("eventDate", event.getDate());
            statsMap.put("eventTime", event.getTime());
            statsMap.put("location", event.getLocation());
            statsMap.put("eventOurScore", event.getOurScore());
            statsMap.put("eventOpponentScore", event.getOpponentScore());
            statsMap.put("eventMatchWon", event.getMatchWon());
            statsMap.put("eventCategoryId", event.getCategoryId());
        }

        return statsMap;
    }
}

