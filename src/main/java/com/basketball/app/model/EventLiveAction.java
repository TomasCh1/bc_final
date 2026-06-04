package com.basketball.app.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(
    name = "event_live_actions",
    indexes = {
        @Index(name = "idx_event_live_actions_event_id", columnList = "event_id"),
        @Index(name = "idx_event_live_actions_event_created", columnList = "event_id,created_at,id"),
        @Index(name = "idx_event_live_actions_event_opp_jersey", columnList = "event_id,opponent_jersey_number")
    }
)
@Data
@NoArgsConstructor
@AllArgsConstructor
public class EventLiveAction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "event_id", nullable = false)
    private Long eventId;

    @Column(name = "period", nullable = false)
    private Integer period;

    @Column(name = "clock_seconds", nullable = false)
    private Integer clockSeconds;

    @Enumerated(EnumType.STRING)
    @Column(name = "team_side", nullable = false, length = 20)
    private TeamSide teamSide;

    @Enumerated(EnumType.STRING)
    @Column(name = "action_type", nullable = false, length = 20)
    private ActionType actionType;

    @Column(name = "our_player_id")
    private Long ourPlayerId;

    @Column(name = "opponent_jersey_number")
    private Integer opponentJerseyNumber;

    @Column(name = "points")
    private Integer points;

    @Column(name = "on_court_player_ids_json", columnDefinition = "TEXT")
    private String onCourtPlayerIdsJson;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "created_by")
    private Long createdBy;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }

    public enum TeamSide {
        OUR,
        OPPONENT
    }

    public enum ActionType {
        SCORE,
        FOUL,
        SUB_IN,
        SUB_OUT
    }
}
