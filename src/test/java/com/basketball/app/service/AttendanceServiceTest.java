package com.basketball.app.service;

import com.basketball.app.dto.AttendanceRecordRequest;
import com.basketball.app.dto.BulkAttendanceRecordRequest;
import com.basketball.app.model.Attendance;
import com.basketball.app.model.Event;
import com.basketball.app.model.User;
import com.basketball.app.repository.AttendanceRepository;
import com.basketball.app.repository.EventRepository;
import com.basketball.app.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AttendanceServiceTest {

    @Mock
    private AttendanceRepository attendanceRepository;

    @Mock
    private EventRepository eventRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private SecurityContext securityContext;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private AttendanceService attendanceService;

    private Event testEvent;
    private User testPlayer;
    private User testTrainer;
    private Attendance testAttendance;

    @BeforeEach
    void setUp() {
        testEvent = new Event();
        testEvent.setId(1L);
        testEvent.setName("Training");
        testEvent.setDate(LocalDate.now());

        testPlayer = new User();
        testPlayer.setId(1L);
        testPlayer.setName("Player");
        testPlayer.setEmail("player@test.com");
        testPlayer.setRole(User.Role.PLAYER);

        testTrainer = new User();
        testTrainer.setId(2L);
        testTrainer.setName("Trainer");
        testTrainer.setEmail("trainer@test.com");
        testTrainer.setRole(User.Role.TRAINER);

        testAttendance = new Attendance();
        testAttendance.setId(1L);
        testAttendance.setEventId(1L);
        testAttendance.setPlayerId(1L);
        testAttendance.setTrainerId(2L);
        testAttendance.setStatus(Attendance.AttendanceStatus.PRESENT);
        testAttendance.setTimestamp(LocalDateTime.now());
        
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private void setupSecurityContext() {
        SecurityContextHolder.setContext(securityContext);
        lenient().when(securityContext.getAuthentication()).thenReturn(authentication);
        lenient().when(authentication.getName()).thenReturn("trainer@test.com");
        lenient().when(userRepository.findByEmailAndDeletedFalse("trainer@test.com")).thenReturn(Optional.of(testTrainer));
    }

    @Test
    void testRecordAttendance_Success() {
        setupSecurityContext();

        AttendanceRecordRequest request = new AttendanceRecordRequest();
        request.setEventId(1L);
        request.setPlayerId(1L);
        request.setStatus(Attendance.AttendanceStatus.PRESENT);
        request.setNotes("On time");

        Attendance savedAttendance = new Attendance();
        savedAttendance.setId(1L);
        savedAttendance.setEventId(1L);
        savedAttendance.setPlayerId(1L);
        savedAttendance.setStatus(Attendance.AttendanceStatus.PRESENT);

        when(eventRepository.findById(1L)).thenReturn(Optional.of(testEvent));
        when(userRepository.findById(1L)).thenReturn(Optional.of(testPlayer));
        when(attendanceRepository.findByEventIdAndPlayerId(1L, 1L)).thenReturn(Optional.empty());
        when(attendanceRepository.save(any(Attendance.class))).thenReturn(savedAttendance);

        Attendance result = attendanceService.recordAttendance(request);

        assertNotNull(result);
        assertEquals(Attendance.AttendanceStatus.PRESENT, result.getStatus());
        verify(eventRepository).findById(1L);
        verify(userRepository).findById(1L);
        verify(attendanceRepository).save(any(Attendance.class));
    }

    @Test
    void testRecordAttendance_UpdateExisting() {
        setupSecurityContext();

        AttendanceRecordRequest request = new AttendanceRecordRequest();
        request.setEventId(1L);
        request.setPlayerId(1L);
        request.setStatus(Attendance.AttendanceStatus.LATE);

        when(eventRepository.findById(1L)).thenReturn(Optional.of(testEvent));
        when(userRepository.findById(1L)).thenReturn(Optional.of(testPlayer));
        when(attendanceRepository.findByEventIdAndPlayerId(1L, 1L)).thenReturn(Optional.of(testAttendance));
        when(attendanceRepository.save(any(Attendance.class))).thenReturn(testAttendance);

        Attendance result = attendanceService.recordAttendance(request);

        assertNotNull(result);
        assertEquals(Attendance.AttendanceStatus.LATE, result.getStatus());
        verify(attendanceRepository).save(any(Attendance.class));
    }

    @Test
    void testRecordAttendance_EventNotFound() {
        AttendanceRecordRequest request = new AttendanceRecordRequest();
        request.setEventId(999L);
        request.setPlayerId(1L);

        when(eventRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> attendanceService.recordAttendance(request));
        verify(attendanceRepository, never()).save(any(Attendance.class));
    }

    @Test
    void testRecordAttendance_PlayerNotFound() {
        AttendanceRecordRequest request = new AttendanceRecordRequest();
        request.setEventId(1L);
        request.setPlayerId(999L);

        when(eventRepository.findById(1L)).thenReturn(Optional.of(testEvent));
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> attendanceService.recordAttendance(request));
    }

    @Test
    void testRecordAttendance_NotAPlayer() {
        AttendanceRecordRequest request = new AttendanceRecordRequest();
        request.setEventId(1L);
        request.setPlayerId(2L);

        when(eventRepository.findById(1L)).thenReturn(Optional.of(testEvent));
        when(userRepository.findById(2L)).thenReturn(Optional.of(testTrainer));

        assertThrows(RuntimeException.class, () -> attendanceService.recordAttendance(request));
    }

    @Test
    void testRecordBulkAttendance_Success() {
        setupSecurityContext();

        BulkAttendanceRecordRequest request = new BulkAttendanceRecordRequest();
        request.setEventId(1L);
        
        Map<String, BulkAttendanceRecordRequest.AttendanceRecordItem> attendanceMap = new HashMap<>();
        
        BulkAttendanceRecordRequest.AttendanceRecordItem item1 = new BulkAttendanceRecordRequest.AttendanceRecordItem();
        item1.setStatus("PRESENT");
        attendanceMap.put("1", item1);
        
        BulkAttendanceRecordRequest.AttendanceRecordItem item2 = new BulkAttendanceRecordRequest.AttendanceRecordItem();
        item2.setStatus("ABSENT");
        attendanceMap.put("3", item2);
        
        request.setAttendance(attendanceMap);

        User player2 = new User();
        player2.setId(3L);
        player2.setRole(User.Role.PLAYER);

        Attendance saved1 = new Attendance();
        saved1.setId(1L);
        Attendance saved2 = new Attendance();
        saved2.setId(2L);

        when(eventRepository.findById(1L)).thenReturn(Optional.of(testEvent));
        when(userRepository.findById(1L)).thenReturn(Optional.of(testPlayer));
        when(userRepository.findById(3L)).thenReturn(Optional.of(player2));
        when(attendanceRepository.findByEventIdAndPlayerId(anyLong(), anyLong())).thenReturn(Optional.empty());
        when(attendanceRepository.save(any(Attendance.class))).thenReturn(saved1, saved2);

        List<Attendance> result = attendanceService.recordBulkAttendance(request);

        assertNotNull(result);
        assertEquals(2, result.size());
        verify(eventRepository).findById(1L);
        verify(attendanceRepository, times(2)).save(any(Attendance.class));
    }

    @Test
    void testFindById_Success() {
        when(attendanceRepository.findById(1L)).thenReturn(Optional.of(testAttendance));

        Optional<Attendance> result = attendanceService.findById(1L);

        assertTrue(result.isPresent());
        assertEquals(1L, result.get().getId());
        verify(attendanceRepository).findById(1L);
    }

    @Test
    void testFindByEvent() {
        List<Attendance> attendances = Arrays.asList(testAttendance);
        when(attendanceRepository.findByEventId(1L)).thenReturn(attendances);

        List<Attendance> result = attendanceService.findByEvent(1L);

        assertEquals(1, result.size());
        verify(attendanceRepository).findByEventId(1L);
    }

    @Test
    void testFindByPlayer() {
        List<Attendance> attendances = Arrays.asList(testAttendance);
        when(attendanceRepository.findByPlayerId(1L)).thenReturn(attendances);

        List<Attendance> result = attendanceService.findByPlayer(1L);

        assertEquals(1, result.size());
        verify(attendanceRepository).findByPlayerId(1L);
    }

    @Test
    void testFindByPlayerAndDateRange() {
        LocalDate start = LocalDate.now().minusDays(7);
        LocalDate end = LocalDate.now().plusDays(7);

        List<Attendance> attendances = Arrays.asList(testAttendance);
        when(attendanceRepository.findByPlayerIdAndDateRange(1L, start, end)).thenReturn(attendances);

        List<Attendance> result = attendanceService.findByPlayerAndDateRange(1L, start, end);

        assertEquals(1, result.size());
        verify(attendanceRepository).findByPlayerIdAndDateRange(1L, start, end);
    }

    @Test
    void testFindByCategory() {
        List<Attendance> attendances = Arrays.asList(testAttendance);
        when(attendanceRepository.findByCategoryId(1L)).thenReturn(attendances);

        List<Attendance> result = attendanceService.findByCategory(1L);

        assertEquals(1, result.size());
        verify(attendanceRepository).findByCategoryId(1L);
    }

    @Test
    void testCalculatePlayerStatistics() {
        LocalDate start = LocalDate.now().minusDays(30);
        LocalDate end = LocalDate.now();

        when(attendanceRepository.countByPlayerIdAndDateRange(1L, start, end)).thenReturn(10L);
        when(attendanceRepository.countByPlayerIdAndStatusAndDateRange(
                1L, Attendance.AttendanceStatus.PRESENT, start, end)).thenReturn(8L);
        when(attendanceRepository.countByPlayerIdAndStatusAndDateRange(
                1L, Attendance.AttendanceStatus.ABSENT, start, end)).thenReturn(1L);
        when(attendanceRepository.countByPlayerIdAndStatusAndDateRange(
                1L, Attendance.AttendanceStatus.LATE, start, end)).thenReturn(1L);
        when(attendanceRepository.countByPlayerIdAndStatusAndDateRange(
                1L, Attendance.AttendanceStatus.EXCUSED, start, end)).thenReturn(0L);

        Map<String, Object> stats = attendanceService.calculatePlayerStatistics(1L, start, end);

        assertNotNull(stats);
        assertEquals(10L, stats.get("totalEvents"));
        assertEquals(8L, stats.get("present"));
        assertEquals(1L, stats.get("absent"));
        assertEquals(1L, stats.get("late"));
        assertEquals(0L, stats.get("excused"));
        assertNotNull(stats.get("attendanceRate"));
    }

    @Test
    void testCalculatePlayerStatistics_NoDateRange() {
        when(attendanceRepository.countByPlayerIdAndStatus(1L, Attendance.AttendanceStatus.PRESENT)).thenReturn(1L);
        when(attendanceRepository.countByPlayerIdAndStatus(1L, Attendance.AttendanceStatus.ABSENT)).thenReturn(0L);
        when(attendanceRepository.countByPlayerIdAndStatus(1L, Attendance.AttendanceStatus.LATE)).thenReturn(0L);
        when(attendanceRepository.countByPlayerIdAndStatus(1L, Attendance.AttendanceStatus.EXCUSED)).thenReturn(0L);

        Map<String, Object> stats = attendanceService.calculatePlayerStatistics(1L, null, null);

        assertNotNull(stats);
        assertEquals(1L, stats.get("present"));
    }
}
