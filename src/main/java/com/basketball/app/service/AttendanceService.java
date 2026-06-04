package com.basketball.app.service;

import com.basketball.app.dto.AttendanceRecordRequest;
import com.basketball.app.dto.BulkAttendanceRecordRequest;
import com.basketball.app.model.Attendance;
import com.basketball.app.model.User;
import com.basketball.app.repository.AttendanceRepository;
import com.basketball.app.repository.EventRepository;
import com.basketball.app.repository.UserRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class AttendanceService {

    private final AttendanceRepository attendanceRepository;
    private final EventRepository eventRepository;
    private final UserRepository userRepository;

    public AttendanceService(AttendanceRepository attendanceRepository,
                            EventRepository eventRepository,
                            UserRepository userRepository) {
        this.attendanceRepository = attendanceRepository;
        this.eventRepository = eventRepository;
        this.userRepository = userRepository;
    }

    /**
     * Get current authenticated user ID
     */
    private Long getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getName() != null) {
            Optional<User> user = userRepository.findByEmailAndDeletedFalse(authentication.getName());
            return user.map(User::getId).orElse(null);
        }
        return null;
    }

    /**
     * Record attendance for a single player
     */
    @Transactional
    public Attendance recordAttendance(AttendanceRecordRequest request) {
        // Validate event exists
        eventRepository.findById(request.getEventId())
                .orElseThrow(() -> new RuntimeException("Event not found: " + request.getEventId()));

        // Validate player exists and is not deleted
        User player = userRepository.findById(request.getPlayerId())
                .orElseThrow(() -> new RuntimeException("Player not found: " + request.getPlayerId()));
        if (Boolean.TRUE.equals(player.getDeleted())) {
            throw new RuntimeException("Player not found: " + request.getPlayerId());
        }
        if (player.getRole() != User.Role.PLAYER) {
            throw new RuntimeException("User is not a player");
        }

        Long trainerId = getCurrentUserId();

        // Check if attendance already exists
        Optional<Attendance> existingAttendance = attendanceRepository.findByEventIdAndPlayerId(
                request.getEventId(), request.getPlayerId());

        Attendance attendance;
        if (existingAttendance.isPresent()) {
            attendance = existingAttendance.get();
            attendance.setStatus(request.getStatus());
            if (request.getNotes() != null) {
                attendance.setNotes(request.getNotes());
            }
            if (trainerId != null) {
                attendance.setTrainerId(trainerId);
            }
        } else {
            attendance = new Attendance();
            attendance.setEventId(request.getEventId());
            attendance.setPlayerId(request.getPlayerId());
            attendance.setStatus(request.getStatus());
            attendance.setNotes(request.getNotes());
            attendance.setTrainerId(trainerId);
        }

        return attendanceRepository.save(attendance);
    }

    /**
     * Record attendance for multiple players
     */
    @Transactional
    public List<Attendance> recordBulkAttendance(BulkAttendanceRecordRequest request) {
        // Validate event exists
        eventRepository.findById(request.getEventId())
                .orElseThrow(() -> new RuntimeException("Event not found: " + request.getEventId()));

        Long trainerId = getCurrentUserId();
        List<Attendance> attendanceRecords = new ArrayList<>();

        for (Map.Entry<String, BulkAttendanceRecordRequest.AttendanceRecordItem> entry : 
                request.getAttendance().entrySet()) {
            
            // Convert String key to Long
            Long playerId;
            try {
                playerId = Long.parseLong(entry.getKey());
            } catch (NumberFormatException e) {
                throw new RuntimeException("Invalid player ID: " + entry.getKey());
            }
            
            BulkAttendanceRecordRequest.AttendanceRecordItem item = entry.getValue();

            // Validate player exists and is not deleted
            User player = userRepository.findById(playerId)
                    .orElseThrow(() -> new RuntimeException("Player not found: " + playerId));
            if (Boolean.TRUE.equals(player.getDeleted())) {
                throw new RuntimeException("Player not found: " + playerId);
            }
            if (player.getRole() != User.Role.PLAYER) {
                continue; // Skip non-players
            }

            // Parse status
            Attendance.AttendanceStatus status;
            try {
                status = Attendance.AttendanceStatus.valueOf(item.getStatus().toUpperCase());
            } catch (IllegalArgumentException e) {
                throw new RuntimeException("Invalid attendance status: " + item.getStatus());
            }

            // Check if attendance already exists
            Optional<Attendance> existingAttendance = attendanceRepository.findByEventIdAndPlayerId(
                    request.getEventId(), playerId);

            Attendance attendance;
            if (existingAttendance.isPresent()) {
                attendance = existingAttendance.get();
                attendance.setStatus(status);
                if (item.getNotes() != null) {
                    attendance.setNotes(item.getNotes());
                }
                if (trainerId != null) {
                    attendance.setTrainerId(trainerId);
                }
            } else {
                attendance = new Attendance();
                attendance.setEventId(request.getEventId());
                attendance.setPlayerId(playerId);
                attendance.setStatus(status);
                attendance.setNotes(item.getNotes());
                attendance.setTrainerId(trainerId);
            }

            attendanceRecords.add(attendanceRepository.save(attendance));
        }

        return attendanceRecords;
    }

    /**
     * Get attendance by ID
     */
    public Optional<Attendance> findById(Long id) {
        return attendanceRepository.findById(id);
    }

    /**
     * Get attendance for an event
     */
    public List<Attendance> findByEvent(Long eventId) {
        return attendanceRepository.findByEventId(eventId);
    }

    /**
     * Get attendance for a player
     */
    public List<Attendance> findByPlayer(Long playerId) {
        return attendanceRepository.findByPlayerId(playerId);
    }

    /**
     * Get attendance by player and date range
     */
    public List<Attendance> findByPlayerAndDateRange(Long playerId, LocalDate startDate, LocalDate endDate) {
        return attendanceRepository.findByPlayerIdAndDateRange(playerId, startDate, endDate);
    }

    /**
     * Get attendance by category
     */
    public List<Attendance> findByCategory(Long categoryId) {
        return attendanceRepository.findByCategoryId(categoryId);
    }

    /**
     * Get attendance for events in a date range (all players)
     */
    public List<Attendance> findByDateRange(LocalDate startDate, LocalDate endDate) {
        if (startDate == null || endDate == null) return List.of();
        return attendanceRepository.findByDateRange(startDate, endDate);
    }

    /**
     * Calculate attendance statistics for a player
     */
    public Map<String, Object> calculatePlayerStatistics(Long playerId, LocalDate startDate, LocalDate endDate) {
        Map<String, Object> stats = new HashMap<>();

        Long totalEvents;
        if (startDate != null && endDate != null) {
            totalEvents = attendanceRepository.countByPlayerIdAndDateRange(playerId, startDate, endDate);
        } else {
            List<Attendance> allAttendance = attendanceRepository.findByPlayerId(playerId);
            totalEvents = (long) allAttendance.size();
        }

        Long present = attendanceRepository.countByPlayerIdAndStatus(playerId, Attendance.AttendanceStatus.PRESENT);
        Long absent = attendanceRepository.countByPlayerIdAndStatus(playerId, Attendance.AttendanceStatus.ABSENT);
        Long late = attendanceRepository.countByPlayerIdAndStatus(playerId, Attendance.AttendanceStatus.LATE);
        Long excused = attendanceRepository.countByPlayerIdAndStatus(playerId, Attendance.AttendanceStatus.EXCUSED);

        if (startDate != null && endDate != null) {
            present = attendanceRepository.countByPlayerIdAndStatusAndDateRange(
                    playerId, Attendance.AttendanceStatus.PRESENT, startDate, endDate);
            absent = attendanceRepository.countByPlayerIdAndStatusAndDateRange(
                    playerId, Attendance.AttendanceStatus.ABSENT, startDate, endDate);
            late = attendanceRepository.countByPlayerIdAndStatusAndDateRange(
                    playerId, Attendance.AttendanceStatus.LATE, startDate, endDate);
            excused = attendanceRepository.countByPlayerIdAndStatusAndDateRange(
                    playerId, Attendance.AttendanceStatus.EXCUSED, startDate, endDate);
            totalEvents = attendanceRepository.countByPlayerIdAndDateRange(playerId, startDate, endDate);
        }

        stats.put("totalEvents", totalEvents);
        stats.put("present", present);
        stats.put("absent", absent);
        stats.put("late", late);
        stats.put("excused", excused);
        stats.put("attendanceRate", totalEvents > 0 ? (double) (present + excused) / totalEvents * 100 : 0.0);

        return stats;
    }

    /**
     * Convert Attendance to map with enriched data
     */
    public Map<String, Object> toAttendanceMap(Attendance attendance) {
        Optional<User> player = userRepository.findById(attendance.getPlayerId());
        Optional<com.basketball.app.model.Event> event = eventRepository.findById(attendance.getEventId());
        return toAttendanceMap(attendance, player.orElse(null), event.orElse(null));
    }

    /**
     * Convert Attendance to map with enriched data using pre-loaded entity caches
     */
    public Map<String, Object> toAttendanceMap(Attendance attendance, User player, com.basketball.app.model.Event event) {
        Map<String, Object> attendanceMap = new HashMap<>();
        attendanceMap.put("id", attendance.getId());
        attendanceMap.put("eventId", attendance.getEventId());
        attendanceMap.put("playerId", attendance.getPlayerId());
        attendanceMap.put("trainerId", attendance.getTrainerId());
        attendanceMap.put("status", attendance.getStatus());
        attendanceMap.put("notes", attendance.getNotes());
        attendanceMap.put("timestamp", attendance.getTimestamp());

        if (player != null) {
            attendanceMap.put("playerName", com.basketball.app.util.UserNames.getDisplayName(player));
            attendanceMap.put("playerEmail", player.getEmail());
        }
        if (event != null) {
            attendanceMap.put("eventName", event.getName());
            attendanceMap.put("eventType", event.getType());
            attendanceMap.put("eventDate", event.getDate());
            attendanceMap.put("eventTime", event.getTime());
            attendanceMap.put("location", event.getLocation());
            attendanceMap.put("opponent", event.getOpponent());
        }

        return attendanceMap;
    }

    /**
     * Find attendance in date range and return as enriched maps in bulk (avoids N+1 queries).
     */
    public List<Map<String, Object>> findByDateRangeAsMaps(LocalDate startDate, LocalDate endDate) {
        if (startDate == null || endDate == null) return List.of();
        List<Attendance> records = attendanceRepository.findByDateRange(startDate, endDate);
        if (records.isEmpty()) return List.of();

        Set<Long> playerIds = records.stream().map(Attendance::getPlayerId).collect(Collectors.toSet());
        Set<Long> eventIds = records.stream().map(Attendance::getEventId).collect(Collectors.toSet());

        Map<Long, User> playersById = userRepository.findAllById(playerIds).stream()
                .collect(Collectors.toMap(User::getId, u -> u));
        Map<Long, com.basketball.app.model.Event> eventsById = eventRepository.findAllById(eventIds).stream()
                .collect(Collectors.toMap(com.basketball.app.model.Event::getId, e -> e));

        return records.stream()
                .map(a -> toAttendanceMap(a, playersById.get(a.getPlayerId()), eventsById.get(a.getEventId())))
                .collect(Collectors.toList());
    }
}