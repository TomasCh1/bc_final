package com.basketball.app.repository;

import com.basketball.app.model.Event;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest(showSql = false)
@ActiveProfiles("test")
class EventRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private EventRepository eventRepository;

    private Event testTraining;
    private Event testMatch;
    private Event testEventWithCategory;

    @BeforeEach
    void setUp() {
        testTraining = createTestEvent("Training Session", Event.EventType.TRAINING, 
                LocalDate.now().plusDays(1), LocalTime.of(18, 0), "Gym A", null, null);
        
        testMatch = createTestEvent("Match vs Team B", Event.EventType.MATCH, 
                LocalDate.now().plusDays(7), LocalTime.of(19, 0), "Arena", 1L, "Team B");
        
        testEventWithCategory = createTestEvent("Category Training", Event.EventType.TRAINING,
                LocalDate.now().plusDays(2), LocalTime.of(17, 0), "Gym B", 1L, null);
    }

    @Test
    void testSaveAndFindById() {
        Event saved = entityManager.persistAndFlush(testTraining);
        Event found = eventRepository.findById(saved.getId()).orElseThrow();
        
        assertEquals("Training Session", found.getName());
        assertEquals(Event.EventType.TRAINING, found.getType());
        assertEquals("Gym A", found.getLocation());
    }

    @Test
    void testFindByType() {
        entityManager.persistAndFlush(testTraining);
        entityManager.persistAndFlush(testMatch);
        entityManager.flush();

        List<Event> trainings = eventRepository.findByType(Event.EventType.TRAINING);
        assertEquals(1, trainings.size());
        assertEquals(Event.EventType.TRAINING, trainings.get(0).getType());

        List<Event> matches = eventRepository.findByType(Event.EventType.MATCH);
        assertEquals(1, matches.size());
        assertEquals(Event.EventType.MATCH, matches.get(0).getType());
    }

    @Test
    void testFindByDate() {
        LocalDate today = LocalDate.now();
        Event todayEvent = createTestEvent("Today Event", Event.EventType.TRAINING,
                today, LocalTime.of(10, 0), "Location", null, null);
        
        entityManager.persistAndFlush(todayEvent);
        entityManager.persistAndFlush(testTraining);
        entityManager.flush();

        List<Event> todayEvents = eventRepository.findByDate(today);
        assertEquals(1, todayEvents.size());
        assertEquals(today, todayEvents.get(0).getDate());
    }

    @Test
    void testFindByDateBetween() {
        LocalDate start = LocalDate.now();
        LocalDate end = LocalDate.now().plusDays(10);
        
        Event event1 = createTestEvent("Event 1", Event.EventType.TRAINING,
                start.plusDays(1), LocalTime.of(10, 0), "Location", null, null);
        Event event2 = createTestEvent("Event 2", Event.EventType.TRAINING,
                start.plusDays(5), LocalTime.of(10, 0), "Location", null, null);
        Event event3 = createTestEvent("Event 3", Event.EventType.TRAINING,
                start.plusDays(15), LocalTime.of(10, 0), "Location", null, null);
        
        entityManager.persistAndFlush(event1);
        entityManager.persistAndFlush(event2);
        entityManager.persistAndFlush(event3);
        entityManager.flush();

        List<Event> eventsInRange = eventRepository.findByDateBetween(start, end);
        assertEquals(2, eventsInRange.size());
        assertTrue(eventsInRange.stream().allMatch(e -> 
            !e.getDate().isBefore(start) && !e.getDate().isAfter(end)));
    }

    @Test
    void testFindByCategoryId() {
        entityManager.persistAndFlush(testEventWithCategory);
        entityManager.persistAndFlush(testTraining);
        entityManager.flush();

        List<Event> categoryEvents = eventRepository.findByCategoryId(1L);
        assertEquals(1, categoryEvents.size());
        assertEquals(1L, categoryEvents.get(0).getCategoryId());
    }

    @Test
    void testFindByCategoryIdAndType() {
        Event matchWithCategory = createTestEvent("Match", Event.EventType.MATCH,
                LocalDate.now().plusDays(3), LocalTime.of(19, 0), "Arena", 1L, "Opponent");
        
        entityManager.persistAndFlush(testEventWithCategory);
        entityManager.persistAndFlush(matchWithCategory);
        entityManager.flush();

        List<Event> trainingInCategory = eventRepository.findByCategoryIdAndType(1L, Event.EventType.TRAINING);
        assertEquals(1, trainingInCategory.size());
        assertEquals(Event.EventType.TRAINING, trainingInCategory.get(0).getType());
    }

    @Test
    void testFindByCategoryIdAndDateBetween() {
        LocalDate start = LocalDate.now();
        LocalDate end = LocalDate.now().plusDays(5);
        
        Event event1 = createTestEvent("Event 1", Event.EventType.TRAINING,
                start.plusDays(1), LocalTime.of(10, 0), "Location", 1L, null);
        Event event2 = createTestEvent("Event 2", Event.EventType.TRAINING,
                start.plusDays(10), LocalTime.of(10, 0), "Location", 1L, null);
        
        entityManager.persistAndFlush(event1);
        entityManager.persistAndFlush(event2);
        entityManager.flush();

        List<Event> events = eventRepository.findByCategoryIdAndDateBetween(1L, start, end);
        assertEquals(1, events.size());
        assertEquals(1L, events.get(0).getCategoryId());
    }

    @Test
    void testFindWithFilters_AllFilters() {
        LocalDate start = LocalDate.now();
        LocalDate end = LocalDate.now().plusDays(10);
        
        entityManager.persistAndFlush(testEventWithCategory);
        entityManager.persistAndFlush(testMatch);
        entityManager.persistAndFlush(testTraining);
        entityManager.flush();

        List<Event> results = eventRepository.findWithFilters(
                1L, Event.EventType.TRAINING, start, end, null);
        
        assertEquals(1, results.size());
        assertEquals(1L, results.get(0).getCategoryId());
        assertEquals(Event.EventType.TRAINING, results.get(0).getType());
    }

    @Test
    void testFindWithFilters_BySearch() {
        Event event1 = createTestEvent("Basketball Training", Event.EventType.TRAINING,
                LocalDate.now().plusDays(1), LocalTime.of(10, 0), "Main Gym", null, null);
        Event event2 = createTestEvent("Match Game", Event.EventType.MATCH,
                LocalDate.now().plusDays(2), LocalTime.of(19, 0), "Arena", null, "Team Alpha");
        
        entityManager.persistAndFlush(event1);
        entityManager.persistAndFlush(event2);
        entityManager.flush();

        List<Event> results = eventRepository.findWithFilters(null, null, null, null, "Basketball");
        assertEquals(1, results.size());
        assertTrue(results.get(0).getName().contains("Basketball"));
    }

    @Test
    void testUpdateEvent() {
        Event saved = entityManager.persistAndFlush(testTraining);
        saved.setName("Updated Training");
        saved.setLocation("New Location");
        Event updated = eventRepository.save(saved);
        
        assertEquals("Updated Training", updated.getName());
        assertEquals("New Location", updated.getLocation());
    }

    @Test
    void testDeleteEvent() {
        Event saved = entityManager.persistAndFlush(testTraining);
        Long id = saved.getId();
        
        eventRepository.deleteById(id);
        entityManager.flush();
        
        assertFalse(eventRepository.findById(id).isPresent());
    }

    @Test
    void testEventTimestamps() {
        Event saved = entityManager.persistAndFlush(testTraining);
        assertNotNull(saved.getCreatedAt());
        assertNotNull(saved.getUpdatedAt());
        
        LocalDateTime originalUpdated = saved.getUpdatedAt();
        
        try {
            Thread.sleep(10);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        saved.setName("Updated");
        Event updated = eventRepository.save(saved);
        entityManager.flush();
        
        assertTrue(updated.getUpdatedAt().isAfter(originalUpdated));
    }

    @Test
    void testMatchWithOpponent() {
        Event saved = entityManager.persistAndFlush(testMatch);
        assertEquals("Team B", saved.getOpponent());
        assertEquals(Event.EventType.MATCH, saved.getType());
    }

    @Test
    void testTrainingWithoutOpponent() {
        Event saved = entityManager.persistAndFlush(testTraining);
        assertNull(saved.getOpponent());
        assertEquals(Event.EventType.TRAINING, saved.getType());
    }

    private Event createTestEvent(String name, Event.EventType type, LocalDate date, 
                                  LocalTime time, String location, Long categoryId, String opponent) {
        Event event = new Event();
        event.setName(name);
        event.setType(type);
        event.setDate(date);
        event.setTime(time);
        event.setLocation(location);
        event.setCategoryId(categoryId);
        event.setOpponent(opponent);
        return event;
    }
}
