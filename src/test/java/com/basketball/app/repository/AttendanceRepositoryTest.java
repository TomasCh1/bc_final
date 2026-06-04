package com.basketball.app.repository;

import com.basketball.app.model.Attendance;
import com.basketball.app.model.Event;
import com.basketball.app.model.User;
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
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest(showSql = false)
@ActiveProfiles("test")
class AttendanceRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private AttendanceRepository attendanceRepository;

    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private UserRepository userRepository;

    private Event testEvent;
    private Event testEvent2;
    private User testPlayer;
    private User testPlayer2;
    private User testTrainer;
    private Attendance testAttendance;

    @BeforeEach
    void setUp() {
        testPlayer = createTestUser("Player 1", "player1@test.com", "password", User.Role.PLAYER);
        testPlayer2 = createTestUser("Player 2", "player2@test.com", "password", User.Role.PLAYER);
        testTrainer = createTestUser("Trainer", "trainer@test.com", "password", User.Role.TRAINER);
        
        entityManager.persistAndFlush(testPlayer);
        entityManager.persistAndFlush(testPlayer2);
        entityManager.persistAndFlush(testTrainer);

        testEvent = createTestEvent("Training", Event.EventType.TRAINING, 
                LocalDate.now(), LocalTime.of(18, 0), "Gym", null, null);
        testEvent2 = createTestEvent("Match", Event.EventType.MATCH,
                LocalDate.now().plusDays(1), LocalTime.of(19, 0), "Arena", 1L, "Opponent");
        
        entityManager.persistAndFlush(testEvent);
        entityManager.persistAndFlush(testEvent2);

        testAttendance = createTestAttendance(testEvent.getId(), testPlayer.getId(), 
                testTrainer.getId(), Attendance.AttendanceStatus.PRESENT);
    }

    @Test
    void testSaveAndFindById() {
        Attendance saved = entityManager.persistAndFlush(testAttendance);
        Attendance found = attendanceRepository.findById(saved.getId()).orElseThrow();
        
        assertEquals(testEvent.getId(), found.getEventId());
        assertEquals(testPlayer.getId(), found.getPlayerId());
        assertEquals(Attendance.AttendanceStatus.PRESENT, found.getStatus());
    }

    @Test
    void testFindByEventId() {
        Attendance attendance2 = createTestAttendance(testEvent.getId(), testPlayer2.getId(),
                testTrainer.getId(), Attendance.AttendanceStatus.ABSENT);
        
        entityManager.persistAndFlush(testAttendance);
        entityManager.persistAndFlush(attendance2);
        entityManager.flush();

        List<Attendance> eventAttendances = attendanceRepository.findByEventId(testEvent.getId());
        assertEquals(2, eventAttendances.size());
        assertTrue(eventAttendances.stream().allMatch(a -> a.getEventId().equals(testEvent.getId())));
    }

    @Test
    void testFindByPlayerId() {
        Attendance attendance2 = createTestAttendance(testEvent2.getId(), testPlayer.getId(),
                testTrainer.getId(), Attendance.AttendanceStatus.LATE);
        
        entityManager.persistAndFlush(testAttendance);
        entityManager.persistAndFlush(attendance2);
        entityManager.flush();

        List<Attendance> playerAttendances = attendanceRepository.findByPlayerId(testPlayer.getId());
        assertEquals(2, playerAttendances.size());
        assertTrue(playerAttendances.stream().allMatch(a -> a.getPlayerId().equals(testPlayer.getId())));
    }

    @Test
    void testFindByEventIdAndPlayerId() {
        entityManager.persistAndFlush(testAttendance);
        entityManager.flush();

        Optional<Attendance> found = attendanceRepository.findByEventIdAndPlayerId(
                testEvent.getId(), testPlayer.getId());
        
        assertTrue(found.isPresent());
        assertEquals(testEvent.getId(), found.get().getEventId());
        assertEquals(testPlayer.getId(), found.get().getPlayerId());
    }

    @Test
    void testFindByEventIdAndPlayerId_NotFound() {
        Optional<Attendance> found = attendanceRepository.findByEventIdAndPlayerId(999L, 999L);
        assertFalse(found.isPresent());
    }

    @Test
    void testFindByPlayerIdAndStatus() {
        Attendance absent = createTestAttendance(testEvent2.getId(), testPlayer.getId(),
                testTrainer.getId(), Attendance.AttendanceStatus.ABSENT);
        
        entityManager.persistAndFlush(testAttendance);
        entityManager.persistAndFlush(absent);
        entityManager.flush();

        List<Attendance> presentAttendances = attendanceRepository.findByPlayerIdAndStatus(
                testPlayer.getId(), Attendance.AttendanceStatus.PRESENT);
        assertEquals(1, presentAttendances.size());
        assertEquals(Attendance.AttendanceStatus.PRESENT, presentAttendances.get(0).getStatus());
    }

    @Test
    void testFindByDateRange() {
        LocalDate start = LocalDate.now().minusDays(1);
        LocalDate end = LocalDate.now().plusDays(2);
        
        entityManager.persistAndFlush(testAttendance);
        entityManager.flush();

        List<Attendance> attendances = attendanceRepository.findByDateRange(start, end);
        assertTrue(attendances.size() >= 1);
    }

    @Test
    void testFindByPlayerIdAndDateRange() {
        LocalDate start = LocalDate.now().minusDays(1);
        LocalDate end = LocalDate.now().plusDays(2);
        
        entityManager.persistAndFlush(testAttendance);
        entityManager.flush();

        List<Attendance> attendances = attendanceRepository.findByPlayerIdAndDateRange(
                testPlayer.getId(), start, end);
        assertTrue(attendances.size() >= 1);
        assertTrue(attendances.stream().allMatch(a -> a.getPlayerId().equals(testPlayer.getId())));
    }

    @Test
    void testFindByCategoryId() {
        entityManager.persistAndFlush(testAttendance);
        entityManager.flush();

        List<Attendance> attendances = attendanceRepository.findByCategoryId(1L);
        assertNotNull(attendances);
    }

    @Test
    void testCountByPlayerIdAndStatus() {
        Attendance absent = createTestAttendance(testEvent2.getId(), testPlayer.getId(),
                testTrainer.getId(), Attendance.AttendanceStatus.ABSENT);
        
        entityManager.persistAndFlush(testAttendance);
        entityManager.persistAndFlush(absent);
        entityManager.flush();

        Long presentCount = attendanceRepository.countByPlayerIdAndStatus(
                testPlayer.getId(), Attendance.AttendanceStatus.PRESENT);
        assertEquals(1L, presentCount);
    }

    @Test
    void testCountByPlayerIdAndDateRange() {
        LocalDate start = LocalDate.now().minusDays(1);
        LocalDate end = LocalDate.now().plusDays(2);
        
        entityManager.persistAndFlush(testAttendance);
        entityManager.flush();

        Long count = attendanceRepository.countByPlayerIdAndDateRange(
                testPlayer.getId(), start, end);
        assertTrue(count >= 1);
    }

    @Test
    void testCountByPlayerIdAndStatusAndDateRange() {
        LocalDate start = LocalDate.now().minusDays(1);
        LocalDate end = LocalDate.now().plusDays(2);
        
        entityManager.persistAndFlush(testAttendance);
        entityManager.flush();

        Long count = attendanceRepository.countByPlayerIdAndStatusAndDateRange(
                testPlayer.getId(), Attendance.AttendanceStatus.PRESENT, start, end);
        assertTrue(count >= 1);
    }

    @Test
    void testUniqueConstraint_EventIdAndPlayerId() {
        entityManager.persistAndFlush(testAttendance);
        entityManager.flush();
        
        Attendance duplicate = createTestAttendance(testEvent.getId(), testPlayer.getId(),
                testTrainer.getId(), Attendance.AttendanceStatus.ABSENT);
        
        assertThrows(Exception.class, () -> {
            entityManager.persistAndFlush(duplicate);
        });
    }

    @Test
    void testUpdateAttendance() {
        Attendance saved = entityManager.persistAndFlush(testAttendance);
        saved.setStatus(Attendance.AttendanceStatus.LATE);
        saved.setNotes("Updated notes");
        Attendance updated = attendanceRepository.save(saved);
        
        assertEquals(Attendance.AttendanceStatus.LATE, updated.getStatus());
        assertEquals("Updated notes", updated.getNotes());
    }

    @Test
    void testDeleteAttendance() {
        Attendance saved = entityManager.persistAndFlush(testAttendance);
        Long id = saved.getId();
        
        attendanceRepository.deleteById(id);
        entityManager.flush();
        
        assertFalse(attendanceRepository.findById(id).isPresent());
    }

    @Test
    void testAttendanceTimestamp() {
        Attendance saved = entityManager.persistAndFlush(testAttendance);
        assertNotNull(saved.getTimestamp());
    }

    @Test
    void testAllAttendanceStatuses() {
        Attendance present = createTestAttendance(testEvent.getId(), testPlayer.getId(),
                testTrainer.getId(), Attendance.AttendanceStatus.PRESENT);
        Attendance absent = createTestAttendance(testEvent2.getId(), testPlayer.getId(),
                testTrainer.getId(), Attendance.AttendanceStatus.ABSENT);
        Attendance late = createTestAttendance(testEvent.getId(), testPlayer2.getId(),
                testTrainer.getId(), Attendance.AttendanceStatus.LATE);
        Attendance excused = createTestAttendance(testEvent2.getId(), testPlayer2.getId(),
                testTrainer.getId(), Attendance.AttendanceStatus.EXCUSED);
        
        entityManager.persistAndFlush(present);
        entityManager.persistAndFlush(absent);
        entityManager.persistAndFlush(late);
        entityManager.persistAndFlush(excused);
        entityManager.flush();

        assertEquals(1, attendanceRepository.findByPlayerIdAndStatus(
                testPlayer.getId(), Attendance.AttendanceStatus.PRESENT).size());
        assertEquals(1, attendanceRepository.findByPlayerIdAndStatus(
                testPlayer.getId(), Attendance.AttendanceStatus.ABSENT).size());
    }

    private Attendance createTestAttendance(Long eventId, Long playerId, Long trainerId,
                                           Attendance.AttendanceStatus status) {
        Attendance attendance = new Attendance();
        attendance.setEventId(eventId);
        attendance.setPlayerId(playerId);
        attendance.setTrainerId(trainerId);
        attendance.setStatus(status);
        attendance.setTimestamp(LocalDateTime.now());
        return attendance;
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

    private User createTestUser(String name, String email, String password, User.Role role) {
        User user = new User();
        user.setName(name);
        user.setEmail(email);
        user.setPassword(password);
        user.setRole(role);
        user.setIsActive(true);
        return user;
    }
}
