package com.basketball.app.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.Map;

@Data
public class BulkAttendanceRecordRequest {
    @NotNull(message = "Event ID is required")
    private Long eventId;

    @NotNull(message = "Attendance records are required")
    private Map<String, AttendanceRecordItem> attendance;

    @Data
    public static class AttendanceRecordItem {
        private String status; // PRESENT, ABSENT, LATE, EXCUSED
        private String notes;
    }
}

