package com.basketball.app.service;

import com.basketball.app.model.Event;
import com.basketball.app.model.Statistics;
import com.basketball.app.repository.EventRepository;
import com.basketball.app.repository.StatisticsRepository;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@Tag("unit")
class ChartServiceTest {

    @Mock
    private StatisticsRepository statisticsRepository;
    @Mock
    private EventRepository eventRepository;

    @InjectMocks
    private ChartService chartService;

    @Test
    void getTimeSeriesData_AggregatesDailyAverage() {
        Statistics s1 = new Statistics();
        s1.setEventId(1L);
        s1.setPts(10);
        Statistics s2 = new Statistics();
        s2.setEventId(2L);
        s2.setPts(20);
        when(statisticsRepository.findAll()).thenReturn(List.of(s1, s2));

        Event e1 = new Event();
        e1.setId(1L);
        e1.setDate(LocalDate.of(2026, 1, 10));
        Event e2 = new Event();
        e2.setId(2L);
        e2.setDate(LocalDate.of(2026, 1, 10));
        when(eventRepository.findAll()).thenReturn(List.of(e1, e2));

        Map<String, Object> result = chartService.getTimeSeriesData(
                null, "PTS", null, null, null, null
        );

        List<?> points = (List<?>) result.get("dataPoints");
        assertEquals(1, points.size());
        assertEquals(1, result.get("count"));
    }

    @Test
    void getAggregatedStatistics_ComputesTotals() {
        Statistics s = new Statistics();
        s.setEventId(1L);
        s.setPts(12);
        s.setOffReb(4);
        s.setDefReb(6);
        s.setAst(3);
        s.setStl(1);
        s.setBlk(2);
        s.setTurnovers(2);
        when(statisticsRepository.findAll()).thenReturn(List.of(s));

        Event e = new Event();
        e.setId(1L);
        e.setDate(LocalDate.of(2026, 1, 1));
        when(eventRepository.findAll()).thenReturn(List.of(e));

        Map<String, Object> result = chartService.getAggregatedStatistics(null, null, null, null);
        Map<?, ?> aggregates = (Map<?, ?>) result.get("aggregates");

        assertEquals(1, result.get("totalRecords"));
        assertEquals(8, aggregates.size());
    }
}
