package com.basketball.app.service;

import com.basketball.app.model.Attendance;
import com.basketball.app.model.Category;
import com.basketball.app.model.Event;
import com.basketball.app.model.User;
import com.basketball.app.repository.CategoryRepository;
import com.basketball.app.repository.EventRepository;
import com.basketball.app.repository.UserRepository;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@Tag("unit")
class AttendanceExportServiceTest {

    @Mock
    private AttendanceService attendanceService;
    @Mock
    private UserRepository userRepository;
    @Mock
    private EventRepository eventRepository;
    @Mock
    private CategoryRepository categoryRepository;

    @InjectMocks
    private AttendanceExportService service;

    @Test
    void exportToExcel_WithRecords_ReturnsWorkbookBytes() throws Exception {
        Attendance attendance = new Attendance();
        attendance.setEventId(1L);
        attendance.setPlayerId(2L);
        attendance.setTrainerId(3L);
        attendance.setStatus(Attendance.AttendanceStatus.PRESENT);

        Event event = new Event();
        event.setId(1L);
        event.setCategoryId(7L);
        Category category = new Category();
        category.setName("U13");
        User trainer = new User();
        trainer.setName("Coach");

        when(attendanceService.findByDateRange(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 31)))
                .thenReturn(List.of(attendance));
        when(attendanceService.toAttendanceMap(attendance)).thenReturn(Map.of(
                "eventDate", LocalDate.of(2026, 1, 10),
                "eventName", "Training",
                "playerName", "Player"
        ));
        when(eventRepository.findById(1L)).thenReturn(Optional.of(event));
        when(categoryRepository.findById(7L)).thenReturn(Optional.of(category));
        when(userRepository.findById(3L)).thenReturn(Optional.of(trainer));

        byte[] bytes = service.exportToExcel(null, null, null, LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 31));
        assertTrue(bytes.length > 100);
    }
}
