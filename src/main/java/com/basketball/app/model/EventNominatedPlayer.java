package com.basketball.app.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(
    name = "event_nominated_players",
    uniqueConstraints = {
        @UniqueConstraint(columnNames = {"event_id", "player_id"})
    },
    indexes = {
        @Index(name = "idx_event_nominated_players_event_id", columnList = "event_id"),
        @Index(name = "idx_event_nominated_players_player_id", columnList = "player_id")
    }
)
@Data
@NoArgsConstructor
@AllArgsConstructor
public class EventNominatedPlayer {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "event_id", nullable = false)
    private Long eventId;

    @Column(name = "player_id", nullable = false)
    private Long playerId;

    @Column(name = "jersey_number")
    private Integer jerseyNumber;

    @Column(name = "starting_five", nullable = false)
    private Boolean startingFive = false;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}
