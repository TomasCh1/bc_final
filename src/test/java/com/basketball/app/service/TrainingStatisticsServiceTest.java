package com.basketball.app.service;

import com.basketball.app.dto.TrainingStatisticsCreateRequest;
import com.basketball.app.dto.TrainingStatisticsUpdateRequest;
import com.basketball.app.model.*;
import com.basketball.app.repository.TrainingStatisticsRepository;
import com.basketball.app.repository.UserRepository;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@Tag("unit")
class TrainingStatisticsServiceTest {

    @Mock
    private TrainingStatisticsRepository trainingStatisticsRepository;
    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private TrainingStatisticsService service;

    @Test
    void createTrainingStatistics_Strelba_ComputesSuccessRate() {
        User user = new User();
        user.setDeleted(false);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(trainingStatisticsRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        TrainingStatisticsCreateRequest request = new TrainingStatisticsCreateRequest();
        request.setUserId(1L);
        request.setExerciseName("Corner shots");
        request.setCategory(TrainingStatistics.TrainingCategory.STRELBA);
        request.setShotsAttempted(20);
        request.setShotsMade(15);
        request.setCourtSpot("Left corner");

        TrainingStatistics result = service.createTrainingStatistics(request);

        assertInstanceOf(StrelbaStats.class, result);
        assertEquals(new BigDecimal("75.00"), ((StrelbaStats) result).getSuccessRate());
    }

    @Test
    void createTrainingStatistics_Beh_InvalidTime_Throws() {
        User user = new User();
        user.setDeleted(false);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        TrainingStatisticsCreateRequest request = new TrainingStatisticsCreateRequest();
        request.setUserId(1L);
        request.setExerciseName("Sprint");
        request.setCategory(TrainingStatistics.TrainingCategory.BEH);
        request.setDistance(new BigDecimal("100"));
        request.setTime("bad-format");

        assertThrows(RuntimeException.class, () -> service.createTrainingStatistics(request));
    }

    @Test
    void updateTrainingStatistics_ExistingCategory_UpdatesOnlyProvidedFields() {
        StrelbaStats existing = new StrelbaStats();
        existing.setShotsAttempted(10);
        existing.setShotsMade(5);
        existing.setExerciseName("Old");
        when(trainingStatisticsRepository.findById(9L)).thenReturn(Optional.of(existing));
        when(trainingStatisticsRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        TrainingStatisticsUpdateRequest request = new TrainingStatisticsUpdateRequest();
        request.setExerciseName("Updated");
        request.setShotsAttempted(20);
        request.setShotsMade(10);

        TrainingStatistics updated = service.updateTrainingStatistics(9L, request);

        assertEquals("Updated", updated.getExerciseName());
        assertEquals(new BigDecimal("50.00"), ((StrelbaStats) updated).getSuccessRate());
    }

    @Test
    void findByUserId_SortsByWorkoutDateDesc() {
        PoslnovanieStats old = new PoslnovanieStats();
        old.setWorkoutDate(LocalDate.of(2025, 1, 1));
        BehStats recent = new BehStats();
        recent.setWorkoutDate(LocalDate.of(2025, 2, 1));
        when(trainingStatisticsRepository.findByUserId(7L)).thenReturn(List.of(old, recent));

        List<TrainingStatistics> results = service.findByUserId(7L);

        assertEquals(recent, results.get(0));
        assertEquals(old, results.get(1));
    }

    @Test
    void deleteTrainingStatistics_Missing_Throws() {
        when(trainingStatisticsRepository.existsById(404L)).thenReturn(false);
        assertThrows(RuntimeException.class, () -> service.deleteTrainingStatistics(404L));
    }
}
