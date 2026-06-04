package com.basketball.app.controller;

import com.basketball.app.dto.AttendanceRecordRequest;
import com.basketball.app.model.Attendance;
import com.basketball.app.model.Event;
import com.basketball.app.service.AttendanceService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(value = AttendanceController.class, excludeAutoConfiguration = SecurityAutoConfiguration.class)
class AttendanceControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AttendanceService attendanceService;

    @MockBean
    private com.basketball.app.service.AttendanceExportService exportService;

    @MockBean
    private com.basketball.app.security.JwtTokenProvider jwtTokenProvider;

    @MockBean
    private com.basketball.app.security.CustomUserDetailsService userDetailsService;

    @Autowired
    private ObjectMapper objectMapper;

    private Attendance testAttendance;
    private Map<String, Object> attendanceMap;

    @BeforeEach
    void setUp() {
        testAttendance = new Attendance();
        testAttendance.setId(1L);
        testAttendance.setEventId(1L);
        testAttendance.setPlayerId(1L);
        testAttendance.setStatus(Attendance.AttendanceStatus.PRESENT);
        testAttendance.setTimestamp(LocalDateTime.now());

        attendanceMap = new HashMap<>();
        attendanceMap.put("id", 1L);
        attendanceMap.put("eventId", 1L);
        attendanceMap.put("playerId", 1L);
        attendanceMap.put("status", "PRESENT");
    }

    @Test
    void testRecordAttendance_Success() throws Exception {
        AttendanceRecordRequest request = new AttendanceRecordRequest();
        request.setEventId(1L);
        request.setPlayerId(1L);
        request.setStatus(Attendance.AttendanceStatus.PRESENT);

        when(attendanceService.recordAttendance(any(AttendanceRecordRequest.class))).thenReturn(testAttendance);
        when(attendanceService.toAttendanceMap(any(Attendance.class))).thenReturn(attendanceMap);

        mockMvc.perform(post("/api/attendance")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.status").value("PRESENT"));

        verify(attendanceService).recordAttendance(any(AttendanceRecordRequest.class));
    }

    @Test
    void testGetAttendanceById_Success() throws Exception {
        when(attendanceService.findById(1L)).thenReturn(Optional.of(testAttendance));
        when(attendanceService.toAttendanceMap(any(Attendance.class))).thenReturn(attendanceMap);

        mockMvc.perform(get("/api/attendance/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.status").value("PRESENT"));

        verify(attendanceService).findById(1L);
    }

    @Test
    void testGetAttendanceById_NotFound() throws Exception {
        when(attendanceService.findById(999L)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/attendance/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").exists());

        verify(attendanceService).findById(999L);
    }

    @Test
    void testGetAttendanceByEvent() throws Exception {
        List<Attendance> attendances = Arrays.asList(testAttendance);
        when(attendanceService.findByEvent(1L)).thenReturn(attendances);
        when(attendanceService.toAttendanceMap(any(Attendance.class))).thenReturn(attendanceMap);

        mockMvc.perform(get("/api/attendance/event/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].eventId").value(1L));

        verify(attendanceService).findByEvent(1L);
    }

    @Test
    void testGetAttendanceByPlayer() throws Exception {
        List<Attendance> attendances = Arrays.asList(testAttendance);
        when(attendanceService.findByPlayer(1L)).thenReturn(attendances);
        when(attendanceService.toAttendanceMap(any(Attendance.class))).thenReturn(attendanceMap);

        mockMvc.perform(get("/api/attendance/player/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].playerId").value(1L));

        verify(attendanceService).findByPlayer(1L);
    }

    @Test
    void testGetPlayerStatistics() throws Exception {
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalEvents", 10L);
        stats.put("present", 8L);
        stats.put("absent", 1L);
        stats.put("late", 1L);
        stats.put("excused", 0L);
        stats.put("attendanceRate", 80.0);

        LocalDate start = LocalDate.now().minusDays(30);
        LocalDate end = LocalDate.now();

        when(attendanceService.calculatePlayerStatistics(eq(1L), eq(start), eq(end))).thenReturn(stats);

        mockMvc.perform(get("/api/attendance/player/1/statistics")
                        .param("startDate", start.toString())
                        .param("endDate", end.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalEvents").value(10L))
                .andExpect(jsonPath("$.attendanceRate").value(80.0));

        verify(attendanceService).calculatePlayerStatistics(eq(1L), eq(start), eq(end));
    }

    @Test
    void testRecordBulkAttendance_Success() throws Exception {
        when(attendanceService.recordBulkAttendance(any())).thenReturn(List.of(testAttendance));
        when(attendanceService.toAttendanceMap(any())).thenReturn(attendanceMap);

        mockMvc.perform(post("/api/attendance/bulk")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"eventId":1,"attendance":{"10":{"status":"PRESENT"},"11":{"status":"ABSENT"}}}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$[0].status").value("PRESENT"));
    }

    @Test
    void testGetAttendanceByPlayerAndDateRange_Success() throws Exception {
        LocalDate start = LocalDate.now().minusDays(7);
        LocalDate end = LocalDate.now();
        when(attendanceService.findByPlayerAndDateRange(1L, start, end)).thenReturn(List.of(testAttendance));
        when(attendanceService.toAttendanceMap(any())).thenReturn(attendanceMap);

        mockMvc.perform(get("/api/attendance/player/1/range")
                        .param("startDate", start.toString())
                        .param("endDate", end.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].playerId").value(1));
    }

    @Test
    void testGetAttendanceByDateRange_Success() throws Exception {
        LocalDate start = LocalDate.now().minusDays(30);
        LocalDate end = LocalDate.now();
        when(attendanceService.findByDateRangeAsMaps(start, end)).thenReturn(List.of(attendanceMap));

        mockMvc.perform(get("/api/attendance/range")
                        .param("startDate", start.toString())
                        .param("endDate", end.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].eventId").value(1));
    }

    @Test
    void testGetAttendanceByCategory_Success() throws Exception {
        when(attendanceService.findByCategory(1L)).thenReturn(List.of(testAttendance));
        when(attendanceService.toAttendanceMap(any())).thenReturn(attendanceMap);

        mockMvc.perform(get("/api/attendance/category/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].eventId").value(1));
    }

    @Test
    void testExportToExcel_Success() throws Exception {
        when(exportService.exportToExcel(any(), any(), any(), any(), any())).thenReturn(new byte[]{1, 2, 3});

        mockMvc.perform(get("/api/attendance/export/excel").param("categoryId", "1"))
                .andExpect(status().isOk());
    }

    @Test
    void testExportToPdf_Failure_ReturnsBadRequest() throws Exception {
        when(exportService.exportToPdf(any(), any(), any(), any(), any()))
                .thenThrow(new RuntimeException("pdf error"));

        mockMvc.perform(get("/api/attendance/export/pdf"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Failed to export attendance: pdf error"));
    }

    @Test
    void testRecordAttendance_ServiceError_ReturnsBadRequest() throws Exception {
        when(attendanceService.recordAttendance(any())).thenThrow(new RuntimeException("duplicate record"));

        AttendanceRecordRequest request = new AttendanceRecordRequest();
        request.setEventId(1L);
        request.setPlayerId(1L);
        request.setStatus(Attendance.AttendanceStatus.PRESENT);

        mockMvc.perform(post("/api/attendance")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("duplicate record"));
    }
}
