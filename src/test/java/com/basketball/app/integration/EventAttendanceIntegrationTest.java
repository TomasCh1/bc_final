package com.basketball.app.integration;

import com.basketball.app.dto.AttendanceRecordRequest;
import com.basketball.app.dto.CategoryCreateRequest;
import com.basketball.app.dto.EventCreateRequest;
import com.basketball.app.dto.UserCreateRequest;
import com.basketball.app.model.Attendance;
import com.basketball.app.model.Category;
import com.basketball.app.model.Event;
import com.basketball.app.model.User;
import com.basketball.app.repository.*;
import com.basketball.app.service.*;
import com.icegreen.greenmail.junit5.GreenMailExtension;
import com.icegreen.greenmail.util.ServerSetupTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Integration test for complete event and attendance workflow:
 * 1. Create category
 * 2. Create trainer and players
 * 3. Create event
 * 4. Record attendance for players
 * 5. Verify attendance statistics
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
@Tag("integration")
class EventAttendanceIntegrationTest {

    @RegisterExtension
    static final GreenMailExtension GREEN_MAIL =
            new GreenMailExtension(ServerSetupTest.SMTP)
                    .withPerMethodLifecycle(false);

    @Autowired
    private CategoryService categoryService;

    @Autowired
    private UserService userService;

    @Autowired
    private EventService eventService;

    @Autowired
    private AttendanceService attendanceService;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private AttendanceRepository attendanceRepository;

    private Category testCategory;
    private User testTrainer;
    private User testPlayer1;
    private User testPlayer2;
    private Event testEvent;

    @BeforeEach
    void setUp() throws Exception {
        attendanceRepository.deleteAll();
        eventRepository.deleteAll();
        userRepository.deleteAll();
        categoryRepository.deleteAll();
        GREEN_MAIL.purgeEmailFromAllMailboxes();

        SecurityContext securityContext = mock(SecurityContext.class);
        Authentication authentication = mock(Authentication.class);
        SecurityContextHolder.setContext(securityContext);
        when(securityContext.getAuthentication()).thenReturn(authentication);

        CategoryCreateRequest categoryRequest = new CategoryCreateRequest();
        categoryRequest.setName("U13");
        categoryRequest.setSeason("2024-2025");
        testCategory = categoryService.createCategory(categoryRequest);

        UserCreateRequest trainerRequest = new UserCreateRequest();
        trainerRequest.setName("Trainer");
        trainerRequest.setEmail("trainer@test.com");
        trainerRequest.setRole(User.Role.TRAINER);
        trainerRequest.setCategoryIds(List.of(testCategory.getId()));
        Map<String, Object> trainerResult = userService.createUser(trainerRequest);
        assertTrue((Boolean) trainerResult.get("credentialsEmailSent"));
        Map<?, ?> trainerMap = (Map<?, ?>) trainerResult.get("user");
        Long trainerId = ((Number) trainerMap.get("id")).longValue();
        testTrainer = userRepository.findById(trainerId).orElseThrow();

        when(authentication.getName()).thenReturn("trainer@test.com");


        UserCreateRequest player1Request = new UserCreateRequest();
        player1Request.setName("Player 1");
        player1Request.setEmail("player1@test.com");
        player1Request.setRole(User.Role.PLAYER);
        player1Request.setCategoryIds(List.of(testCategory.getId()));
        Map<String, Object> player1Result = userService.createUser(player1Request);
        assertTrue((Boolean) player1Result.get("credentialsEmailSent"));
        Map<?, ?> player1Map = (Map<?, ?>) player1Result.get("user");
        Long player1Id = ((Number) player1Map.get("id")).longValue();
        testPlayer1 = userRepository.findById(player1Id).orElseThrow();

        UserCreateRequest player2Request = new UserCreateRequest();
        player2Request.setName("Player 2");
        player2Request.setEmail("player2@test.com");
        player2Request.setRole(User.Role.PLAYER);
        player2Request.setCategoryIds(List.of(testCategory.getId()));
        Map<String, Object> player2Result = userService.createUser(player2Request);
        assertTrue((Boolean) player2Result.get("credentialsEmailSent"));
        Map<?, ?> player2Map = (Map<?, ?>) player2Result.get("user");
        Long player2Id = ((Number) player2Map.get("id")).longValue();
        testPlayer2 = userRepository.findById(player2Id).orElseThrow();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void testCompleteEventAttendanceWorkflow() {
        EventCreateRequest eventRequest = new EventCreateRequest();
        eventRequest.setName("Training Session");
        eventRequest.setType(Event.EventType.TRAINING);
        eventRequest.setDate(LocalDate.now().plusDays(1));
        eventRequest.setTime(LocalTime.of(18, 0));
        eventRequest.setLocation("Gym A");
        eventRequest.setCategoryId(testCategory.getId());

        Map<String, Object> eventResult = eventService.createEventWithDetails(eventRequest);
        Map<?, ?> eventMap = (Map<?, ?>) eventResult.get("event");
        Long eventId = ((Number) eventMap.get("id")).longValue();
        testEvent = eventRepository.findById(eventId).orElseThrow();

        assertNotNull(testEvent);
        assertEquals("Training Session", testEvent.getName());
        assertEquals(Event.EventType.TRAINING, testEvent.getType());

        AttendanceRecordRequest attendance1Request = new AttendanceRecordRequest();
        attendance1Request.setEventId(eventId);
        attendance1Request.setPlayerId(testPlayer1.getId());
        attendance1Request.setStatus(Attendance.AttendanceStatus.PRESENT);
        attendance1Request.setNotes("On time");

        Attendance attendance1 = attendanceService.recordAttendance(attendance1Request);
        assertNotNull(attendance1);
        assertEquals(Attendance.AttendanceStatus.PRESENT, attendance1.getStatus());
        assertEquals(testPlayer1.getId(), attendance1.getPlayerId());

        AttendanceRecordRequest attendance2Request = new AttendanceRecordRequest();
        attendance2Request.setEventId(eventId);
        attendance2Request.setPlayerId(testPlayer2.getId());
        attendance2Request.setStatus(Attendance.AttendanceStatus.LATE);
        attendance2Request.setNotes("Arrived 10 minutes late");

        Attendance attendance2 = attendanceService.recordAttendance(attendance2Request);
        assertNotNull(attendance2);
        assertEquals(Attendance.AttendanceStatus.LATE, attendance2.getStatus());

        List<Attendance> eventAttendances = attendanceService.findByEvent(eventId);
        assertEquals(2, eventAttendances.size());

        List<Attendance> player1Attendances = attendanceService.findByPlayer(testPlayer1.getId());
        assertEquals(1, player1Attendances.size());
        assertEquals(Attendance.AttendanceStatus.PRESENT, player1Attendances.get(0).getStatus());

        List<Attendance> player2Attendances = attendanceService.findByPlayer(testPlayer2.getId());
        assertEquals(1, player2Attendances.size());
        assertEquals(Attendance.AttendanceStatus.LATE, player2Attendances.get(0).getStatus());

        LocalDate start = LocalDate.now().minusDays(30);
        LocalDate end = LocalDate.now().plusDays(30);
        Map<String, Object> stats = attendanceService.calculatePlayerStatistics(
                testPlayer1.getId(), start, end);
        
        assertNotNull(stats);
        assertTrue(((Number) stats.get("totalEvents")).longValue() >= 1);
        assertTrue(((Number) stats.get("present")).longValue() >= 1);
    }

    @Test
    void testMultipleEventsAndAttendance() {
        EventCreateRequest event1Request = new EventCreateRequest();
        event1Request.setName("Training 1");
        event1Request.setType(Event.EventType.TRAINING);
        event1Request.setDate(LocalDate.now().plusDays(1));
        event1Request.setTime(LocalTime.of(18, 0));
        event1Request.setCategoryId(testCategory.getId());

        Map<String, Object> event1Result = eventService.createEventWithDetails(event1Request);
        Map<?, ?> event1Map = (Map<?, ?>) event1Result.get("event");
        Long event1Id = ((Number) event1Map.get("id")).longValue();

        EventCreateRequest event2Request = new EventCreateRequest();
        event2Request.setName("Training 2");
        event2Request.setType(Event.EventType.TRAINING);
        event2Request.setDate(LocalDate.now().plusDays(3));
        event2Request.setTime(LocalTime.of(18, 0));
        event2Request.setCategoryId(testCategory.getId());

        Map<String, Object> event2Result = eventService.createEventWithDetails(event2Request);
        Map<?, ?> event2Map = (Map<?, ?>) event2Result.get("event");
        Long event2Id = ((Number) event2Map.get("id")).longValue();

        AttendanceRecordRequest att1 = new AttendanceRecordRequest();
        att1.setEventId(event1Id);
        att1.setPlayerId(testPlayer1.getId());
        att1.setStatus(Attendance.AttendanceStatus.PRESENT);
        attendanceService.recordAttendance(att1);

        AttendanceRecordRequest att2 = new AttendanceRecordRequest();
        att2.setEventId(event2Id);
        att2.setPlayerId(testPlayer1.getId());
        att2.setStatus(Attendance.AttendanceStatus.ABSENT);
        attendanceService.recordAttendance(att2);

        List<Attendance> playerAttendances = attendanceService.findByPlayer(testPlayer1.getId());
        assertEquals(2, playerAttendances.size());

        LocalDate start = LocalDate.now().minusDays(30);
        LocalDate end = LocalDate.now().plusDays(30);
        Map<String, Object> stats = attendanceService.calculatePlayerStatistics(
                testPlayer1.getId(), start, end);
        
        assertEquals(2L, stats.get("totalEvents"));
        assertEquals(1L, stats.get("present"));
        assertEquals(1L, stats.get("absent"));
    }

    @Test
    void testUpdateAttendance() {
        EventCreateRequest eventRequest = new EventCreateRequest();
        eventRequest.setName("Training");
        eventRequest.setType(Event.EventType.TRAINING);
        eventRequest.setDate(LocalDate.now().plusDays(1));
        eventRequest.setTime(LocalTime.of(18, 0));
        eventRequest.setCategoryId(testCategory.getId());

        Map<String, Object> eventResult = eventService.createEventWithDetails(eventRequest);
        Map<?, ?> eventMap = (Map<?, ?>) eventResult.get("event");
        Long eventId = ((Number) eventMap.get("id")).longValue();

        AttendanceRecordRequest request = new AttendanceRecordRequest();
        request.setEventId(eventId);
        request.setPlayerId(testPlayer1.getId());
        request.setStatus(Attendance.AttendanceStatus.PRESENT);
        Attendance attendance = attendanceService.recordAttendance(request);

        AttendanceRecordRequest updateRequest = new AttendanceRecordRequest();
        updateRequest.setEventId(eventId);
        updateRequest.setPlayerId(testPlayer1.getId());
        updateRequest.setStatus(Attendance.AttendanceStatus.LATE);
        updateRequest.setNotes("Updated to late");
        
        Attendance updated = attendanceService.recordAttendance(updateRequest);
        assertEquals(Attendance.AttendanceStatus.LATE, updated.getStatus());
        assertEquals(attendance.getId(), updated.getId());
    }
}
