package com.basketball.app.service;

import com.basketball.app.model.Event;
import com.basketball.app.model.Statistics;
import com.basketball.app.repository.StatisticsRepository;
import com.basketball.app.repository.EventRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ChartService {

    private final StatisticsRepository statisticsRepository;
    private final EventRepository eventRepository;

    public ChartService(StatisticsRepository statisticsRepository,
                       EventRepository eventRepository) {
        this.statisticsRepository = statisticsRepository;
        this.eventRepository = eventRepository;
    }

    /**
     * Get time-series data for performance metrics over time
     * Returns data points grouped by date for a specific metric with averages per day
     */
    public Map<String, Object> getTimeSeriesData(Long playerId, String metric, 
                                                 LocalDate startDate, LocalDate endDate,
                                                 Statistics.StatType statType, Long categoryId) {
        List<Statistics> statistics;
        if (playerId != null && categoryId != null) {
            statistics = statisticsRepository.findByPlayerId(playerId);
        } else if (playerId != null) {
            statistics = statisticsRepository.findByPlayerId(playerId);
        } else if (categoryId != null) {
            statistics = statisticsRepository.findByCategoryId(categoryId);
        } else {
            statistics = statisticsRepository.findAll();
        }

        if (statType != null) {
            statistics = statistics.stream()
                .filter(s -> s.getStatType() == statType)
                .collect(Collectors.toList());
        }

        Map<Long, Event> eventMap = eventRepository.findAll().stream()
            .collect(Collectors.toMap(Event::getId, e -> e));

        Map<String, List<Double>> dateValuesMap = new LinkedHashMap<>();
        
        for (Statistics stat : statistics) {
            Event event = eventMap.get(stat.getEventId());
            if (event == null) continue;

            if (startDate != null && event.getDate().isBefore(startDate)) continue;
            if (endDate != null && event.getDate().isAfter(endDate)) continue;
            
            if (categoryId != null && playerId != null) {
                if (event.getCategoryId() == null || !event.getCategoryId().equals(categoryId)) {
                    continue;
                }
            }

            Double metricValue = stat.getMetricValue(metric);
            if (metricValue != null) {
                String dateStr = event.getDate().format(DateTimeFormatter.ISO_LOCAL_DATE);
                dateValuesMap.computeIfAbsent(dateStr, k -> new ArrayList<>()).add(metricValue);
            }
        }

        Map<String, Map<String, Object>> uniqueDataPoints = new LinkedHashMap<>();
        for (Map.Entry<String, List<Double>> entry : dateValuesMap.entrySet()) {
            String date = entry.getKey();
            List<Double> values = entry.getValue();
            
            double average = values.stream()
                .mapToDouble(Double::doubleValue)
                .average()
                .orElse(0.0);
            
            average = Math.round(average * 100.0) / 100.0;
            
            Map<String, Object> point = new HashMap<>();
            point.put("date", date);
            point.put("value", average);
            point.put("count", values.size());
            
            uniqueDataPoints.put(date, point);
        }

        List<Map<String, Object>> dataPoints = new ArrayList<>(uniqueDataPoints.values());
        dataPoints.sort(Comparator.comparing(p -> (String) p.get("date")));

        Map<String, Object> result = new HashMap<>();
        result.put("metric", metric);
        result.put("playerId", playerId);
        result.put("categoryId", categoryId);
        result.put("statType", statType);
        result.put("startDate", startDate != null ? startDate.toString() : null);
        result.put("endDate", endDate != null ? endDate.toString() : null);
        result.put("dataPoints", dataPoints);
        result.put("count", dataPoints.size());

        return result;
    }

    /**
     * Get category distribution data
     * Returns aggregated statistics grouped by category
     */
    public Map<String, Object> getCategoryDistribution(String metric, 
                                                      LocalDate startDate, LocalDate endDate,
                                                      Statistics.StatType statType) {
        List<Statistics> statistics = statisticsRepository.findAll();
        
        if (statType != null) {
            statistics = statistics.stream()
                .filter(s -> s.getStatType() == statType)
                .collect(Collectors.toList());
        }

        Map<Long, Event> eventMap = eventRepository.findAll().stream()
            .collect(Collectors.toMap(Event::getId, e -> e));

        Map<Long, List<Double>> categoryValues = new HashMap<>();
        
        for (Statistics stat : statistics) {
            Event event = eventMap.get(stat.getEventId());
            if (event == null || event.getCategoryId() == null) continue;

            if (startDate != null && event.getDate().isBefore(startDate)) continue;
            if (endDate != null && event.getDate().isAfter(endDate)) continue;

            Double metricValue = stat.getMetricValue(metric);
            if (metricValue != null) {
                categoryValues.computeIfAbsent(event.getCategoryId(), k -> new ArrayList<>())
                    .add(metricValue);
            }
        }

        List<Map<String, Object>> distribution = new ArrayList<>();
        for (Map.Entry<Long, List<Double>> entry : categoryValues.entrySet()) {
            List<Double> values = entry.getValue();
            double sum = values.stream().mapToDouble(Double::doubleValue).sum();
            double avg = values.isEmpty() ? 0 : sum / values.size();
            
            Map<String, Object> categoryData = new HashMap<>();
            categoryData.put("categoryId", entry.getKey());
            categoryData.put("average", Math.round(avg * 100.0) / 100.0);
            categoryData.put("total", Math.round(sum * 100.0) / 100.0);
            categoryData.put("count", values.size());
            distribution.add(categoryData);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("metric", metric);
        result.put("statType", statType);
        result.put("startDate", startDate != null ? startDate.toString() : null);
        result.put("endDate", endDate != null ? endDate.toString() : null);
        result.put("distribution", distribution);
        result.put("totalCategories", distribution.size());

        return result;
    }

    /**
     * Get comparative analysis data
     * Compares players, game vs training, or other dimensions
     */
    public Map<String, Object> getComparativeAnalysis(String metric, String comparisonType,
                                                      List<Long> playerIds, 
                                                      LocalDate startDate, LocalDate endDate,
                                                      Long categoryId) {
        List<Statistics> statistics;
        
        if (categoryId != null) {
            statistics = statisticsRepository.findByCategoryId(categoryId);
        } else {
            statistics = statisticsRepository.findAll();
        }
        
        if (playerIds != null && !playerIds.isEmpty()) {
            statistics = statistics.stream()
                .filter(s -> playerIds.contains(s.getPlayerId()))
                .collect(Collectors.toList());
        }

        Map<Long, Event> eventMap = eventRepository.findAll().stream()
            .collect(Collectors.toMap(Event::getId, e -> e));

        Map<String, Object> result = new HashMap<>();
        result.put("metric", metric);
        result.put("comparisonType", comparisonType);
        result.put("startDate", startDate != null ? startDate.toString() : null);
        result.put("endDate", endDate != null ? endDate.toString() : null);

        if ("players".equals(comparisonType)) {
            Map<Long, List<Double>> playerValues = new HashMap<>();
            
            for (Statistics stat : statistics) {
                Event event = eventMap.get(stat.getEventId());
                if (event == null) continue;

                if (startDate != null && event.getDate().isBefore(startDate)) continue;
                if (endDate != null && event.getDate().isAfter(endDate)) continue;
                
                if (categoryId != null) {
                    if (event.getCategoryId() == null || !event.getCategoryId().equals(categoryId)) {
                        continue;
                    }
                }

                Double metricValue = stat.getMetricValue(metric);
                if (metricValue != null) {
                    playerValues.computeIfAbsent(stat.getPlayerId(), k -> new ArrayList<>())
                        .add(metricValue);
                }
            }

            List<Map<String, Object>> comparison = new ArrayList<>();
            for (Map.Entry<Long, List<Double>> entry : playerValues.entrySet()) {
                List<Double> values = entry.getValue();
                double sum = values.stream().mapToDouble(Double::doubleValue).sum();
                double avg = values.isEmpty() ? 0 : sum / values.size();
                double max = values.isEmpty() ? 0 : Collections.max(values);
                double min = values.isEmpty() ? 0 : Collections.min(values);
                
                Map<String, Object> playerData = new HashMap<>();
                playerData.put("playerId", entry.getKey());
                playerData.put("average", Math.round(avg * 100.0) / 100.0);
                playerData.put("total", Math.round(sum * 100.0) / 100.0);
                playerData.put("max", Math.round(max * 100.0) / 100.0);
                playerData.put("min", Math.round(min * 100.0) / 100.0);
                playerData.put("count", values.size());
                comparison.add(playerData);
            }

            result.put("comparison", comparison);
            result.put("count", comparison.size());

        } else if ("gameVsTraining".equals(comparisonType)) {
            Map<Statistics.StatType, List<Double>> typeValues = new HashMap<>();
            
            for (Statistics stat : statistics) {
                Event event = eventMap.get(stat.getEventId());
                if (event == null) continue;

                if (startDate != null && event.getDate().isBefore(startDate)) continue;
                if (endDate != null && event.getDate().isAfter(endDate)) continue;

                Double metricValue = stat.getMetricValue(metric);
                if (metricValue != null) {
                    typeValues.computeIfAbsent(stat.getStatType(), k -> new ArrayList<>())
                        .add(metricValue);
                }
            }

            List<Map<String, Object>> comparison = new ArrayList<>();
            for (Map.Entry<Statistics.StatType, List<Double>> entry : typeValues.entrySet()) {
                List<Double> values = entry.getValue();
                double sum = values.stream().mapToDouble(Double::doubleValue).sum();
                double avg = values.isEmpty() ? 0 : sum / values.size();
                
                Map<String, Object> typeData = new HashMap<>();
                typeData.put("statType", entry.getKey().name());
                typeData.put("average", Math.round(avg * 100.0) / 100.0);
                typeData.put("total", Math.round(sum * 100.0) / 100.0);
                typeData.put("count", values.size());
                comparison.add(typeData);
            }

            result.put("comparison", comparison);
            result.put("count", comparison.size());
        }

        return result;
    }

    /**
     * Get aggregated statistics summary
     * Returns overall statistics for dashboard
     */
    public Map<String, Object> getAggregatedStatistics(Long playerId, 
                                                       LocalDate startDate, LocalDate endDate,
                                                       Statistics.StatType statType) {
        List<Statistics> statistics = playerId != null 
            ? statisticsRepository.findByPlayerId(playerId)
            : statisticsRepository.findAll();

        if (statType != null) {
            statistics = statistics.stream()
                .filter(s -> s.getStatType() == statType)
                .collect(Collectors.toList());
        }

        Map<Long, Event> eventMap = eventRepository.findAll().stream()
            .collect(Collectors.toMap(Event::getId, e -> e));

        List<Statistics> filteredStats = statistics.stream()
            .filter(stat -> {
                Event event = eventMap.get(stat.getEventId());
                if (event == null) return false;
                if (startDate != null && event.getDate().isBefore(startDate)) return false;
                if (endDate != null && event.getDate().isAfter(endDate)) return false;
                return true;
            })
            .collect(Collectors.toList());

        Map<String, List<Double>> metricValues = new HashMap<>();
        String[] metrics = {"PTS", "OFF", "DEF", "AST", "STL", "BLK", "TO", "INDEX"};

        for (Statistics stat : filteredStats) {
            for (String metric : metrics) {
                Double value = stat.getMetricValue(metric);
                if (value != null) {
                    metricValues.computeIfAbsent(metric, k -> new ArrayList<>())
                        .add(value);
                }
            }
        }

        Map<String, Map<String, Object>> aggregates = new HashMap<>();
        for (Map.Entry<String, List<Double>> entry : metricValues.entrySet()) {
            List<Double> values = entry.getValue();
            double sum = values.stream().mapToDouble(Double::doubleValue).sum();
            double avg = values.isEmpty() ? 0 : sum / values.size();
            double max = values.isEmpty() ? 0 : Collections.max(values);
            double min = values.isEmpty() ? 0 : Collections.min(values);
            
            Map<String, Object> agg = new HashMap<>();
            agg.put("average", Math.round(avg * 100.0) / 100.0);
            agg.put("total", Math.round(sum * 100.0) / 100.0);
            agg.put("max", Math.round(max * 100.0) / 100.0);
            agg.put("min", Math.round(min * 100.0) / 100.0);
            agg.put("count", values.size());
            aggregates.put(entry.getKey(), agg);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("playerId", playerId);
        result.put("statType", statType);
        result.put("startDate", startDate != null ? startDate.toString() : null);
        result.put("endDate", endDate != null ? endDate.toString() : null);
        result.put("totalRecords", filteredStats.size());
        result.put("aggregates", aggregates);

        return result;
    }

}
