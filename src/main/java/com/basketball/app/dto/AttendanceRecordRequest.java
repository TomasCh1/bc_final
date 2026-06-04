package com.basketball.app.dto;

import com.basketball.app.model.Attendance;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class AttendanceRecordRequest {
    @NotNull(message = "Event ID is required")
    private Long eventId;

    @NotNull(message = "Player ID is required")
    private Long playerId;

    @NotNull(message = "Attendance status is required")
    private Attendance.AttendanceStatus status;

    private String notes;
}

