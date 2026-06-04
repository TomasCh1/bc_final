package com.basketball.app.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Base class for training statistics.
 * For match statistics use {@link Statistics}.
 */
@Entity
@Table(name = "training_statistics")
@Inheritance(strategy = InheritanceType.JOINED)
@DiscriminatorColumn(name = "category", discriminatorType = DiscriminatorType.STRING)
@Data
@NoArgsConstructor
public abstract class TrainingStatistics {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "exercise_name", nullable = false, length = 64)
    private String exerciseName;

    @Column(columnDefinition = "TEXT")
    private String note;

    @Column(name = "workout_date", nullable = false)
    private LocalDate workoutDate;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (workoutDate == null) {
            workoutDate = LocalDate.now();
        }
    }

    /** Returns category from discriminator / subclass type. */
    public TrainingCategory getCategory() {
        if (this instanceof PoslnovanieStats) return TrainingCategory.POSLNOVANIE;
        if (this instanceof BehStats) return TrainingCategory.BEH;
        if (this instanceof StrelbaStats) return TrainingCategory.STRELBA;
        return null;
    }

    public enum TrainingCategory {
        POSLNOVANIE, BEH, STRELBA
    }
}
