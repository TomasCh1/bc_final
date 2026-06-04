package com.basketball.app.service;

import com.basketball.app.dto.TrainingStatisticsCreateRequest;
import com.basketball.app.dto.TrainingStatisticsUpdateRequest;
import com.basketball.app.model.*;
import com.basketball.app.repository.TrainingStatisticsRepository;
import com.basketball.app.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class TrainingStatisticsService {

    private final TrainingStatisticsRepository trainingStatisticsRepository;
    private final UserRepository userRepository;

    public TrainingStatisticsService(TrainingStatisticsRepository trainingStatisticsRepository,
                                    UserRepository userRepository) {
        this.trainingStatisticsRepository = trainingStatisticsRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public TrainingStatistics createTrainingStatistics(TrainingStatisticsCreateRequest request) {
        var user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new RuntimeException("User not found: " + request.getUserId()));
        if (Boolean.TRUE.equals(user.getDeleted())) {
            throw new RuntimeException("User not found: " + request.getUserId());
        }

        validateCategoryFields(request.getCategory(), request);

        TrainingStatistics entity = createEntity(request.getCategory());
        setBaseFields(entity, request.getUserId(), request.getExerciseName(), request.getNote(),
                request.getWorkoutDate() != null ? request.getWorkoutDate() : LocalDate.now());

        switch (request.getCategory()) {
            case POSLNOVANIE -> {
                PoslnovanieStats ps = (PoslnovanieStats) entity;
                ps.setWeight(request.getWeight());
                ps.setRepetitions(request.getRepetitions());
            }
            case BEH -> {
                BehStats bs = (BehStats) entity;
                bs.setTime(request.getTime());
                bs.setDistance(request.getDistance());
            }
            case STRELBA -> {
                StrelbaStats ss = (StrelbaStats) entity;
                ss.setShotsAttempted(request.getShotsAttempted());
                ss.setShotsMade(request.getShotsMade());
                ss.setCourtSpot(request.getCourtSpot());
                if (request.getShotsAttempted() != null && request.getShotsAttempted() > 0) {
                    ss.setSuccessRate(BigDecimal.valueOf(request.getShotsMade())
                            .divide(BigDecimal.valueOf(request.getShotsAttempted()), 4, RoundingMode.HALF_UP)
                            .multiply(BigDecimal.valueOf(100))
                            .setScale(2, RoundingMode.HALF_UP));
                }
            }
        }

        return trainingStatisticsRepository.save(entity);
    }

    @Transactional
    public TrainingStatistics updateTrainingStatistics(Long id, TrainingStatisticsUpdateRequest request) {
        TrainingStatistics existing = trainingStatisticsRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Training statistics not found: " + id));

        TrainingStatistics.TrainingCategory category = request.getCategory() != null
                ? request.getCategory()
                : existing.getCategory();

        if (request.getExerciseName() != null) {
            existing.setExerciseName(request.getExerciseName());
        }
        if (request.getNote() != null) {
            existing.setNote(request.getNote());
        }
        if (request.getWorkoutDate() != null) {
            existing.setWorkoutDate(request.getWorkoutDate());
        }

        if (request.getCategory() != null && request.getCategory() != existing.getCategory()) {
            validateCategoryFields(request.getCategory(), request);
            return replaceWithNewCategory(id, request);
        }

        validateCategoryFields(category, request);

        switch (category) {
            case POSLNOVANIE -> {
                PoslnovanieStats ps = (PoslnovanieStats) existing;
                if (request.getWeight() != null) ps.setWeight(request.getWeight());
                if (request.getRepetitions() != null) ps.setRepetitions(request.getRepetitions());
            }
            case BEH -> {
                BehStats bs = (BehStats) existing;
                if (request.getTime() != null) bs.setTime(request.getTime());
                if (request.getDistance() != null) bs.setDistance(request.getDistance());
            }
            case STRELBA -> {
                StrelbaStats ss = (StrelbaStats) existing;
                if (request.getShotsAttempted() != null) ss.setShotsAttempted(request.getShotsAttempted());
                if (request.getShotsMade() != null) ss.setShotsMade(request.getShotsMade());
                if (request.getCourtSpot() != null) ss.setCourtSpot(request.getCourtSpot());
                if ((request.getShotsAttempted() != null || request.getShotsMade() != null)
                        && ss.getShotsAttempted() != null && ss.getShotsAttempted() > 0) {
                    ss.setSuccessRate(BigDecimal.valueOf(ss.getShotsMade())
                            .divide(BigDecimal.valueOf(ss.getShotsAttempted()), 4, RoundingMode.HALF_UP)
                            .multiply(BigDecimal.valueOf(100))
                            .setScale(2, RoundingMode.HALF_UP));
                }
            }
        }

        return trainingStatisticsRepository.save(existing);
    }

    private TrainingStatistics createEntity(TrainingStatistics.TrainingCategory category) {
        return switch (category) {
            case POSLNOVANIE -> new PoslnovanieStats();
            case BEH -> new BehStats();
            case STRELBA -> new StrelbaStats();
        };
    }

    private void setBaseFields(TrainingStatistics entity, Long userId, String exerciseName,
                              String note, LocalDate workoutDate) {
        entity.setUserId(userId);
        entity.setExerciseName(exerciseName);
        entity.setNote(note);
        entity.setWorkoutDate(workoutDate);
    }

    private TrainingStatistics replaceWithNewCategory(Long id, TrainingStatisticsUpdateRequest request) {
        TrainingStatistics existing = trainingStatisticsRepository.findById(id).orElseThrow();
        TrainingStatisticsCreateRequest createReq = new TrainingStatisticsCreateRequest();
        createReq.setUserId(existing.getUserId());
        createReq.setExerciseName(request.getExerciseName() != null ? request.getExerciseName() : existing.getExerciseName());
        createReq.setCategory(request.getCategory());
        createReq.setNote(request.getNote() != null ? request.getNote() : existing.getNote());
        createReq.setWorkoutDate(request.getWorkoutDate() != null ? request.getWorkoutDate() : existing.getWorkoutDate());
        createReq.setWeight(request.getWeight());
        createReq.setRepetitions(request.getRepetitions());
        createReq.setTime(request.getTime());
        createReq.setDistance(request.getDistance());
        createReq.setShotsAttempted(request.getShotsAttempted());
        createReq.setShotsMade(request.getShotsMade());
        createReq.setCourtSpot(request.getCourtSpot());

        trainingStatisticsRepository.deleteById(id);
        return createTrainingStatistics(createReq);
    }

    private void validateCategoryFields(TrainingStatistics.TrainingCategory category, Object request) {
        if (category == TrainingStatistics.TrainingCategory.POSLNOVANIE) {
            if (request instanceof TrainingStatisticsCreateRequest) {
                TrainingStatisticsCreateRequest createReq = (TrainingStatisticsCreateRequest) request;
                if (createReq.getWeight() == null || createReq.getRepetitions() == null) {
                    throw new RuntimeException("Weight and repetitions are required for POSLNOVANIE category");
                }
            }
        } else if (category == TrainingStatistics.TrainingCategory.BEH) {
            if (request instanceof TrainingStatisticsCreateRequest) {
                TrainingStatisticsCreateRequest createReq = (TrainingStatisticsCreateRequest) request;
                if (createReq.getTime() == null || createReq.getDistance() == null) {
                    throw new RuntimeException("Time and distance are required for BEH category");
                }
                if (!isValidTimeFormat(createReq.getTime())) {
                    throw new RuntimeException("Invalid time format. Use hh:mm:ss, mm:ss, or ss.ms format");
                }
            } else if (request instanceof TrainingStatisticsUpdateRequest) {
                TrainingStatisticsUpdateRequest updateReq = (TrainingStatisticsUpdateRequest) request;
                if (updateReq.getTime() != null && !isValidTimeFormat(updateReq.getTime())) {
                    throw new RuntimeException("Invalid time format. Use hh:mm:ss, mm:ss, or ss.ms format");
                }
            }
        } else if (category == TrainingStatistics.TrainingCategory.STRELBA) {
            if (request instanceof TrainingStatisticsCreateRequest) {
                TrainingStatisticsCreateRequest createReq = (TrainingStatisticsCreateRequest) request;
                if (createReq.getShotsAttempted() == null || createReq.getShotsMade() == null || createReq.getCourtSpot() == null) {
                    throw new RuntimeException("Shots attempted, shots made, and court spot are required for STRELBA category");
                }
                if (createReq.getShotsAttempted() <= 0) {
                    throw new RuntimeException("Shots attempted must be greater than 0");
                }
                if (createReq.getShotsMade() < 0) {
                    throw new RuntimeException("Shots made cannot be negative");
                }
                if (createReq.getShotsMade() > createReq.getShotsAttempted()) {
                    throw new RuntimeException("Shots made cannot exceed shots attempted");
                }
            } else if (request instanceof TrainingStatisticsUpdateRequest) {
                TrainingStatisticsUpdateRequest updateReq = (TrainingStatisticsUpdateRequest) request;
                if (updateReq.getShotsAttempted() != null && updateReq.getShotsAttempted() <= 0) {
                    throw new RuntimeException("Shots attempted must be greater than 0");
                }
                if (updateReq.getShotsMade() != null && updateReq.getShotsMade() < 0) {
                    throw new RuntimeException("Shots made cannot be negative");
                }
                if (updateReq.getShotsAttempted() != null && updateReq.getShotsMade() != null
                        && updateReq.getShotsMade() > updateReq.getShotsAttempted()) {
                    throw new RuntimeException("Shots made cannot exceed shots attempted");
                }
            }
        }
    }

    private boolean isValidTimeFormat(String time) {
        if (time == null || time.trim().isEmpty()) return false;
        return time.matches("^\\d{1,2}:\\d{2}:\\d{2}(\\.\\d+)?$")
                || time.matches("^\\d{1,2}:\\d{2}(\\.\\d+)?$")
                || time.matches("^\\d+(\\.\\d+)?$");
    }

    public TrainingStatistics findById(Long id) {
        return trainingStatisticsRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Training statistics not found: " + id));
    }

    public List<TrainingStatistics> findByUserId(Long userId) {
        List<TrainingStatistics> results = trainingStatisticsRepository.findByUserId(userId);
        return results.stream()
                .sorted((a, b) -> {
                    LocalDate dateA = a.getWorkoutDate() != null ? a.getWorkoutDate()
                            : (a.getCreatedAt() != null ? a.getCreatedAt().toLocalDate() : LocalDate.MIN);
                    LocalDate dateB = b.getWorkoutDate() != null ? b.getWorkoutDate()
                            : (b.getCreatedAt() != null ? b.getCreatedAt().toLocalDate() : LocalDate.MIN);
                    return dateB.compareTo(dateA);
                })
                .collect(Collectors.toList());
    }

    public List<TrainingStatistics> findLastNByUserId(Long userId, int limit) {
        return findByUserId(userId).stream().limit(limit).collect(Collectors.toList());
    }

    public List<TrainingStatistics> findWithFilters(Long userId,
                                                    TrainingStatistics.TrainingCategory category,
                                                    LocalDate startDate, LocalDate endDate, String searchTerm) {
        List<TrainingStatistics> results;

        if (userId == null) {
            results = trainingStatisticsRepository.findAll();
        } else if (searchTerm != null && !searchTerm.trim().isEmpty()) {
            results = trainingStatisticsRepository.findByUserIdAndExerciseNameContaining(userId, searchTerm);
        } else {
            results = findByUserId(userId);
        }

        if (category != null) {
            results = results.stream()
                    .filter(t -> t.getCategory() == category)
                    .collect(Collectors.toList());
        }

        if (startDate != null || endDate != null) {
            LocalDate start = startDate != null ? startDate : LocalDate.of(1970, 1, 1);
            LocalDate end = endDate != null ? endDate : LocalDate.now();
            results = results.stream()
                    .filter(t -> {
                        LocalDate wd = t.getWorkoutDate() != null ? t.getWorkoutDate()
                                : (t.getCreatedAt() != null ? t.getCreatedAt().toLocalDate() : null);
                        return wd != null && !wd.isBefore(start) && !wd.isAfter(end);
                    })
                    .collect(Collectors.toList());
        }

        return results.stream()
                .sorted((a, b) -> {
                    LocalDate dateA = a.getWorkoutDate() != null ? a.getWorkoutDate()
                            : (a.getCreatedAt() != null ? a.getCreatedAt().toLocalDate() : LocalDate.MIN);
                    LocalDate dateB = b.getWorkoutDate() != null ? b.getWorkoutDate()
                            : (b.getCreatedAt() != null ? b.getCreatedAt().toLocalDate() : LocalDate.MIN);
                    return dateB.compareTo(dateA);
                })
                .collect(Collectors.toList());
    }

    @Transactional
    public void deleteTrainingStatistics(Long id) {
        if (!trainingStatisticsRepository.existsById(id)) {
            throw new RuntimeException("Training statistics not found: " + id);
        }
        trainingStatisticsRepository.deleteById(id);
    }
}
