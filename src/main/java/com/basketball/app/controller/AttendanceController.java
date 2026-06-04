package com.basketball.app.controller;

import com.basketball.app.dto.AttendanceRecordRequest;
import com.basketball.app.dto.BulkAttendanceRecordRequest;
import com.basketball.app.model.Attendance;
import com.basketball.app.service.AttendanceService;
import com.basketball.app.service.AttendanceExportService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/attendance")
public class AttendanceController {

    private final AttendanceService attendanceService;
    private final AttendanceExportService exportService;

    public AttendanceController(AttendanceService attendanceService, AttendanceExportService exportService) {
        this.attendanceService = attendanceService;
        this.exportService = exportService;
    }

    /**
     * Record attendance for a single player
     */
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'TRAINER')")
    public ResponseEntity<?> recordAttendance(@Valid @RequestBody AttendanceRecordRequest request) {
        try {
            Attendance attendance = attendanceService.recordAttendance(request);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(attendanceService.toAttendanceMap(attendance));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Record attendance for multiple players
     */
    @PostMapping("/bulk")
    @PreAuthorize("hasAnyRole('ADMIN', 'TRAINER')")
    public ResponseEntity<?> recordBulkAttendance(@Valid @RequestBody BulkAttendanceRecordRequest request) {
        try {
            List<Attendance> attendanceRecords = attendanceService.recordBulkAttendance(request);
            List<Map<String, Object>> attendanceMaps = attendanceRecords.stream()
                    .map(attendanceService::toAttendanceMap)
                    .collect(java.util.stream.Collectors.toList());
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(attendanceMaps);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Get attendance by ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<?> getAttendanceById(@PathVariable Long id) {
        try {
            Attendance attendance = attendanceService.findById(id)
                    .orElseThrow(() -> new RuntimeException("Attendance record not found: " + id));
            return ResponseEntity.ok(attendanceService.toAttendanceMap(attendance));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Get attendance for an event
     */
    @GetMapping("/event/{eventId}")
    public ResponseEntity<?> getAttendanceByEvent(@PathVariable Long eventId) {
        try {
            List<Attendance> attendanceRecords = attendanceService.findByEvent(eventId);
            List<Map<String, Object>> attendanceMaps = attendanceRecords.stream()
                    .map(attendanceService::toAttendanceMap)
                    .collect(java.util.stream.Collectors.toList());
            return ResponseEntity.ok(attendanceMaps);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Get attendance for a player
     */
    @GetMapping("/player/{playerId}")
    public ResponseEntity<?> getAttendanceByPlayer(@PathVariable Long playerId) {
        try {
            List<Attendance> attendanceRecords = attendanceService.findByPlayer(playerId);
            List<Map<String, Object>> attendanceMaps = attendanceRecords.stream()
                    .map(attendanceService::toAttendanceMap)
                    .collect(java.util.stream.Collectors.toList());
            return ResponseEntity.ok(attendanceMaps);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Get attendance by player and date range
     */
    @GetMapping("/player/{playerId}/range")
    public ResponseEntity<?> getAttendanceByPlayerAndDateRange(
            @PathVariable Long playerId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        try {
            List<Attendance> attendanceRecords = attendanceService.findByPlayerAndDateRange(
                    playerId, startDate, endDate);
            List<Map<String, Object>> attendanceMaps = attendanceRecords.stream()
                    .map(attendanceService::toAttendanceMap)
                    .collect(java.util.stream.Collectors.toList());
            return ResponseEntity.ok(attendanceMaps);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Get attendance for all players in a date range
     */
    @GetMapping("/range")
    public ResponseEntity<?> getAttendanceByDateRange(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        try {
            List<Map<String, Object>> attendanceMaps = attendanceService.findByDateRangeAsMaps(startDate, endDate);
            return ResponseEntity.ok(attendanceMaps);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Get attendance statistics for a player
     */
    @GetMapping("/player/{playerId}/statistics")
    public ResponseEntity<?> getPlayerStatistics(
            @PathVariable Long playerId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        try {
            Map<String, Object> statistics = attendanceService.calculatePlayerStatistics(
                    playerId, startDate, endDate);
            return ResponseEntity.ok(statistics);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Get attendance by category
     */
    @GetMapping("/category/{categoryId}")
    public ResponseEntity<?> getAttendanceByCategory(@PathVariable Long categoryId) {
        try {
            List<Attendance> attendanceRecords = attendanceService.findByCategory(categoryId);
            List<Map<String, Object>> attendanceMaps = attendanceRecords.stream()
                    .map(attendanceService::toAttendanceMap)
                    .collect(java.util.stream.Collectors.toList());
            return ResponseEntity.ok(attendanceMaps);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Export attendance to Excel
     */
    @GetMapping("/export/excel")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> exportToExcel(
            @RequestParam(required = false) Long playerId,
            @RequestParam(required = false) Long eventId,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        try {
            byte[] excelData = exportService.exportToExcel(playerId, eventId, categoryId, startDate, endDate);
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
            headers.setContentDispositionFormData("attachment", "attendance_report.xlsx");
            
            return ResponseEntity.ok()
                    .headers(headers)
                    .body(excelData);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "Failed to export attendance: " + e.getMessage()));
        }
    }

    /**
     * Export attendance to PDF
     */
    @GetMapping("/export/pdf")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> exportToPdf(
            @RequestParam(required = false) Long playerId,
            @RequestParam(required = false) Long eventId,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        try {
            byte[] pdfData = exportService.exportToPdf(playerId, eventId, categoryId, startDate, endDate);
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.setContentDispositionFormData("attachment", "attendance_report.pdf");
            
            return ResponseEntity.ok()
                    .headers(headers)
                    .body(pdfData);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "Failed to export attendance: " + e.getMessage()));
        }
    }
}

