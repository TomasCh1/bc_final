package com.basketball.app.service;

import com.basketball.app.dto.StatisticsCreateRequest;
import com.basketball.app.dto.StatisticsUpdateRequest;
import com.basketball.app.model.Event;
import com.basketball.app.model.Statistics;
import com.basketball.app.model.User;
import com.basketball.app.repository.EventRepository;
import com.basketball.app.repository.StatisticsRepository;
import com.basketball.app.repository.UserRepository;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@Tag("unit")
class StatisticsServiceTest {

    @Mock
    private StatisticsRepository statisticsRepository;
    @Mock
    private EventRepository eventRepository;
    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private StatisticsService statisticsService;

    @Test
    void calculateIndex_UsesFormulaAndRounds() {
        Map<String, Object> values = new HashMap<>();
        values.put("PTS", 20);
        values.put("OFF", 5);
        values.put("DEF", 6);
        values.put("AST", 4);
        values.put("STL", 2);
        values.put("BLK", 1);
        values.put("FGC", 15);
        values.put("FGÚ", 10);
        values.put("FTC", 8);
        values.put("FTÚ", 6);
        values.put("TO", 3);
        Double index = statisticsService.calculateIndex(values);

        assertEquals(28.0, index);
    }

    @Test
    void createStatistics_ValidRequest_SavesGameStats() {
        when(eventRepository.findById(10L)).thenReturn(Optional.of(new Event()));
        User user = new User();
        user.setDeleted(false);
        when(userRepository.findById(20L)).thenReturn(Optional.of(user));
        when(statisticsRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        StatisticsCreateRequest request = new StatisticsCreateRequest();
        request.setEventId(10L);
        request.setPlayerId(20L);
        request.setOpponent("Rivals");
        Map<String, Object> values = new HashMap<>();
        values.put("PTS", 12);
        values.put("OFF", 2);
        values.put("DEF", 3);
        values.put("AST", 1);
        values.put("STL", 1);
        values.put("BLK", 0);
        values.put("FGC", 8);
        values.put("FGÚ", 5);
        values.put("FTC", 2);
        values.put("FTÚ", 2);
        values.put("TO", 1);
        request.setValues(values);

        Statistics saved = statisticsService.createStatistics(request);

        assertEquals(Statistics.StatType.GAME, saved.getStatType());
        assertEquals("Rivals", saved.getOpponent());
        assertNotNull(saved.getStatIndex());
    }

    @Test
    void findWithFilters_DateRangeAndCategory_FiltersByEventMetadata() {
        Statistics s1 = new Statistics();
        s1.setEventId(1L);
        s1.setStatType(Statistics.StatType.GAME);
        Statistics s2 = new Statistics();
        s2.setEventId(2L);
        s2.setStatType(Statistics.StatType.GAME);
        when(statisticsRepository.findAll()).thenReturn(List.of(s1, s2));

        Event inRange = new Event();
        inRange.setId(1L);
        inRange.setCategoryId(9L);
        inRange.setDate(LocalDate.of(2026, 1, 15));
        inRange.setTime(LocalTime.NOON);
        Event outOfRange = new Event();
        outOfRange.setId(2L);
        outOfRange.setCategoryId(9L);
        outOfRange.setDate(LocalDate.of(2027, 1, 15));
        outOfRange.setTime(LocalTime.NOON);
        when(eventRepository.findAll()).thenReturn(List.of(inRange, outOfRange));

        List<Statistics> filtered = statisticsService.findWithFilters(
                null, null, 9L, Statistics.StatType.GAME,
                LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31), null
        );

        assertEquals(1, filtered.size());
        assertEquals(1L, filtered.get(0).getEventId());
    }

    @Test
    void calculateIndex_StringValues_ParsesAndCalculates() {
        Map<String, Object> values = new HashMap<>();
        values.put("PTS", "10");
        values.put("OFF", "2");
        values.put("DEF", "3");
        values.put("AST", "1");
        values.put("STL", "0");
        values.put("BLK", "0");
        values.put("FGC", "5");
        values.put("FGÚ", "3");
        values.put("FTC", "2");
        values.put("FTÚ", "1");
        values.put("TO", "1");

        assertEquals(12.0, statisticsService.calculateIndex(values));
    }

    @Test
    void calculateIndex_InvalidString_UsesDefaultZero() {
        Map<String, Object> values = new HashMap<>();
        values.put("PTS", "not-a-number");
        values.put("OFF", 0);
        values.put("DEF", 0);
        values.put("AST", 0);
        values.put("STL", 0);
        values.put("BLK", 0);
        values.put("FGC", 0);
        values.put("FGÚ", 0);
        values.put("FTC", 0);
        values.put("FTÚ", 0);
        values.put("TO", 0);

        assertEquals(0.0, statisticsService.calculateIndex(values));
    }

    @Test
    void createStatistics_EventNotFound_Throws() {
        when(eventRepository.findById(99L)).thenReturn(Optional.empty());

        StatisticsCreateRequest request = new StatisticsCreateRequest();
        request.setEventId(99L);
        request.setPlayerId(1L);
        request.setOpponent("X");

        assertThrows(RuntimeException.class, () -> statisticsService.createStatistics(request));
    }

    @Test
    void createStatistics_DeletedPlayer_Throws() {
        when(eventRepository.findById(10L)).thenReturn(Optional.of(new Event()));
        User deleted = new User();
        deleted.setDeleted(true);
        when(userRepository.findById(20L)).thenReturn(Optional.of(deleted));

        StatisticsCreateRequest request = new StatisticsCreateRequest();
        request.setEventId(10L);
        request.setPlayerId(20L);
        request.setOpponent("Rivals");
        request.setValues(Map.of("PTS", 5));

        assertThrows(RuntimeException.class, () -> statisticsService.createStatistics(request));
    }

    @Test
    void createStatistics_WithExplicitIndex_UsesProvidedIndex() {
        when(eventRepository.findById(10L)).thenReturn(Optional.of(new Event()));
        User user = new User();
        user.setDeleted(false);
        when(userRepository.findById(20L)).thenReturn(Optional.of(user));
        when(statisticsRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        Map<String, Object> values = new HashMap<>();
        values.put("PTS", 5);
        values.put("INDEX", 99.5);
        StatisticsCreateRequest request = new StatisticsCreateRequest();
        request.setEventId(10L);
        request.setPlayerId(20L);
        request.setOpponent("Rivals");
        request.setValues(values);

        Statistics saved = statisticsService.createStatistics(request);
        assertEquals(99.5, saved.getStatIndex());
    }

    @Test
    void updateStatistics_UpdatesOpponentAndValues() {
        Statistics existing = new Statistics();
        existing.setId(1L);
        existing.setOpponent("Old");
        when(statisticsRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(statisticsRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        Map<String, Object> values = new HashMap<>();
        values.put("PTS", 8);
        values.put("OFF", 1);
        values.put("DEF", 1);
        values.put("AST", 0);
        values.put("STL", 0);
        values.put("BLK", 0);
        values.put("FGC", 4);
        values.put("FGÚ", 2);
        values.put("FTC", 0);
        values.put("FTÚ", 0);
        values.put("TO", 0);

        StatisticsUpdateRequest request = new StatisticsUpdateRequest();
        request.setOpponent("New Rivals");
        request.setValues(values);

        Statistics updated = statisticsService.updateStatistics(1L, request);
        assertEquals("New Rivals", updated.getOpponent());
        assertEquals(8, updated.getPts());
    }

    @Test
    void updateStatistics_NotFound_Throws() {
        when(statisticsRepository.findById(99L)).thenReturn(Optional.empty());
        assertThrows(RuntimeException.class,
                () -> statisticsService.updateStatistics(99L, new StatisticsUpdateRequest()));
    }

    @Test
    void findWithFilters_ByPlayerId_ReturnsPlayerStats() {
        Statistics s = new Statistics();
        s.setPlayerId(5L);
        when(statisticsRepository.findByPlayerId(5L)).thenReturn(List.of(s));

        List<Statistics> result = statisticsService.findWithFilters(
                5L, null, null, null, null, null, null);

        assertEquals(1, result.size());
        verify(statisticsRepository).findByPlayerId(5L);
    }

    @Test
    void findWithFilters_ByEventId_ReturnsEventStats() {
        Statistics s = new Statistics();
        s.setEventId(3L);
        when(statisticsRepository.findByEventId(3L)).thenReturn(List.of(s));

        List<Statistics> result = statisticsService.findWithFilters(
                null, 3L, null, null, null, null, null);

        assertEquals(1, result.size());
        verify(statisticsRepository).findByEventId(3L);
    }

    @Test
    void findWithFilters_ByCategoryOnly_UsesCategoryRepository() {
        Statistics s = new Statistics();
        when(statisticsRepository.findByCategoryId(7L)).thenReturn(List.of(s));

        List<Statistics> result = statisticsService.findWithFilters(
                null, null, 7L, null, null, null, null);

        assertEquals(1, result.size());
        verify(statisticsRepository).findByCategoryId(7L);
    }

    @Test
    void findWithFilters_OpponentFilter_IsCaseInsensitive() {
        Statistics s1 = new Statistics();
        s1.setOpponent("Team Alpha");
        Statistics s2 = new Statistics();
        s2.setOpponent("Other");
        when(statisticsRepository.findAll()).thenReturn(List.of(s1, s2));

        List<Statistics> result = statisticsService.findWithFilters(
                null, null, null, null, null, null, "team alpha");

        assertEquals(1, result.size());
        assertEquals("Team Alpha", result.get(0).getOpponent());
    }

    @Test
    void findWithFiltersAsMaps_EnrichesPlayerAndEvent() {
        Statistics s = new Statistics();
        s.setId(1L);
        s.setEventId(10L);
        s.setPlayerId(20L);
        s.setStatType(Statistics.StatType.GAME);
        s.setOpponent("Rivals");
        s.setTimestamp(LocalDateTime.now());
        s.setPts(12);
        when(statisticsRepository.findByEventId(10L)).thenReturn(List.of(s));

        Event event = new Event();
        event.setId(10L);
        event.setName("Match");
        event.setType(Event.EventType.MATCH);
        event.setDate(LocalDate.of(2026, 3, 1));
        User player = new User();
        player.setId(20L);
        player.setName("Player One");
        player.setEmail("p@example.com");
        when(eventRepository.findAllById(any())).thenReturn(List.of(event));
        when(userRepository.findAllById(any())).thenReturn(List.of(player));

        List<Map<String, Object>> maps = statisticsService.findWithFiltersAsMaps(
                null, 10L, null, null, null, null, null);

        assertEquals(1, maps.size());
        assertEquals("Player One", maps.get(0).get("playerName"));
        assertEquals("Match", maps.get(0).get("eventName"));
    }

    @Test
    void findRecentGameStatistics_NoEvents_ReturnsEmptyPayload() {
        Page<Long> emptyPage = new PageImpl<>(List.of(), PageRequest.of(0, 5), 0);
        when(eventRepository.findRecentMatchEventIdsWithGameStats(eq(3L), any()))
                .thenReturn(emptyPage);

        Map<String, Object> result = statisticsService.findRecentGameStatistics(3L, 5, 0);

        assertTrue(((List<?>) result.get("statistics")).isEmpty());
        assertEquals(0L, result.get("totalGames"));
        assertEquals(false, result.get("hasMore"));
    }

    @Test
    void findRecentGameStatistics_WithEvents_ReturnsSortedMaps() {
        Page<Long> page = new PageImpl<>(List.of(2L, 1L), PageRequest.of(0, 5), 2);
        when(eventRepository.findRecentMatchEventIdsWithGameStats(eq(null), any())).thenReturn(page);

        Statistics s1 = new Statistics();
        s1.setEventId(1L);
        s1.setPlayerId(10L);
        Statistics s2 = new Statistics();
        s2.setEventId(2L);
        s2.setPlayerId(11L);
        when(statisticsRepository.findByEventIdIn(any())).thenReturn(new ArrayList<>(List.of(s1, s2)));

        Event e1 = new Event();
        e1.setId(1L);
        e1.setName("Game 1");
        Event e2 = new Event();
        e2.setId(2L);
        e2.setName("Game 2");
        when(eventRepository.findAllById(any())).thenReturn(List.of(e1, e2));
        when(userRepository.findAllById(any())).thenReturn(List.of());

        Map<String, Object> result = statisticsService.findRecentGameStatistics(null, 5, 0);

        assertEquals(2, ((List<?>) result.get("statistics")).size());
        assertEquals(2L, result.get("totalGames"));
        assertEquals(false, result.get("hasMore"));
    }

    @Test
    void deleteStatistics_Exists_Deletes() {
        when(statisticsRepository.existsById(1L)).thenReturn(true);
        statisticsService.deleteStatistics(1L);
        verify(statisticsRepository).deleteById(1L);
    }

    @Test
    void deleteStatistics_NotFound_Throws() {
        when(statisticsRepository.existsById(99L)).thenReturn(false);
        assertThrows(RuntimeException.class, () -> statisticsService.deleteStatistics(99L));
    }

    @Test
    void toStatisticsMap_FallbackLoadsFromRepositories() {
        Statistics s = new Statistics();
        s.setId(1L);
        s.setEventId(10L);
        s.setPlayerId(20L);
        s.setStatType(Statistics.StatType.GAME);
        s.setPts(5);

        Event event = new Event();
        event.setId(10L);
        event.setName("Final");
        User player = new User();
        player.setId(20L);
        player.setName("Star");
        when(userRepository.findById(20L)).thenReturn(Optional.of(player));
        when(eventRepository.findById(10L)).thenReturn(Optional.of(event));

        Map<String, Object> map = statisticsService.toStatisticsMap(s);

        assertEquals("Star", map.get("playerName"));
        assertEquals("Final", map.get("eventName"));
    }
}
