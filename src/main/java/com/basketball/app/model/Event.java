package com.basketball.app.model;

import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorColumn;
import jakarta.persistence.DiscriminatorType;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Entity
@Table(
    name = "events",
    indexes = {
        @Index(name = "idx_events_date", columnList = "date"),
        @Index(name = "idx_events_category_date", columnList = "category_id,date")
    }
)
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Event {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, columnDefinition = "VARCHAR(255)")
    private String name;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private EventType type;

    @Column(nullable = false)
    private LocalDate date;

    /** Start time (required for TRAINING when coach/admin records it) */
    private LocalTime time;

    /** End time for training time window (optional) */
    @Column(name = "end_time")
    private LocalTime endTime;

    @Column(columnDefinition = "VARCHAR(255)")
    private String location;

    @Column(name = "category_id")
    private Long categoryId;

    // @ManyToOne(fetch = FetchType.LAZY)
    // @JoinColumn(name = "category_id")

    @Column(columnDefinition = "VARCHAR(255)")
    private String opponent;

    @Column(columnDefinition = "TEXT")
    private String notes;

    /** True if match is at home (location in Trnava), false if away, null if not applicable */
    @Column(name = "is_home_match")
    private Boolean isHomeMatch;

    /** True if match was won, false if lost, null if not yet played or not applicable */
    @Column(name = "match_won")
    private Boolean matchWon;

    /** Our team's score (match only). Null if not yet played or not recorded. */
    @Column(name = "our_score")
    private Integer ourScore;

    /** Opponent's score (match only). Null if not yet played or not recorded. */
    @Column(name = "opponent_score")
    private Integer opponentScore;

    /** True if coach/admin cancelled this training (display as strikethrough + "Zrušené") */
    @Column(name = "cancelled")
    private Boolean cancelled;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public enum EventType {
        TRAINING, MATCH
    }
}

