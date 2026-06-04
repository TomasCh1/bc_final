package com.basketball.app.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;

/**
 * Match statistics only.
 */
@Entity
@Table(name = "statistics")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Statistics {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "event_id", nullable = false)
    private Long eventId;

    @Column(name = "player_id", nullable = false)
    private Long playerId;

    @Column(name = "stat_type", nullable = false)
    @Enumerated(EnumType.STRING)
    private StatType statType;

    @JsonProperty("MIN")
    @Column(name = "min")
    private Double min;

    @JsonProperty("2PÚ")
    @Column(name = "two_p_made")
    private Integer twoPMade;

    @JsonProperty("2PC")
    @Column(name = "two_p_attempts")
    private Integer twoPAttempts;

    @JsonProperty("3PÚ")
    @Column(name = "three_p_made")
    private Integer threePMade;

    @JsonProperty("3PC")
    @Column(name = "three_p_attempts")
    private Integer threePAttempts;

    @JsonProperty("FGÚ")
    @Column(name = "fg_made")
    private Integer fgMade;

    @JsonProperty("FGC")
    @Column(name = "fg_attempts")
    private Integer fgAttempts;

    @JsonProperty("FTÚ")
    @Column(name = "ft_made")
    private Integer ftMade;

    @JsonProperty("FTC")
    @Column(name = "ft_attempts")
    private Integer ftAttempts;

    @JsonProperty("PTS")
    @Column(name = "pts")
    private Integer pts;

    @JsonProperty("F+")
    @Column(name = "fouls_plus")
    private Integer foulsPlus;

    @JsonProperty("F-")
    @Column(name = "fouls_minus")
    private Integer foulsMinus;

    @JsonProperty("OFF")
    @Column(name = "off_reb")
    private Integer offReb;

    @JsonProperty("DEF")
    @Column(name = "def_reb")
    private Integer defReb;

    @JsonProperty("STL")
    @Column(name = "stl")
    private Integer stl;

    @JsonProperty("AST")
    @Column(name = "ast")
    private Integer ast;

    @JsonProperty("BLK")
    @Column(name = "blk")
    private Integer blk;

    @JsonProperty("TO")
    @Column(name = "turnovers")
    private Integer turnovers;

    @JsonProperty("INDEX")
    @Column(name = "stat_index")
    private Double statIndex;

    @Column(nullable = false)
    private String opponent;

    @Column(name = "timestamp")
    private LocalDateTime timestamp;

    @PrePersist
    protected void onCreate() {
        if (timestamp == null) {
            timestamp = LocalDateTime.now();
        }
    }

    private Double calculateIndex() {
        double pts = this.pts != null ? this.pts : 0;
        double off = offReb != null ? offReb : 0;
        double def = defReb != null ? defReb : 0;
        double ast = this.ast != null ? this.ast : 0;
        double stl = this.stl != null ? this.stl : 0;
        double blk = this.blk != null ? this.blk : 0;
        double fgc = fgAttempts != null ? fgAttempts : 0;
        double fgu = fgMade != null ? fgMade : 0;
        double ftc = ftAttempts != null ? ftAttempts : 0;
        double ftu = ftMade != null ? ftMade : 0;
        double to = turnovers != null ? turnovers : 0;
        double index = (pts + off + def + ast + stl + blk) - ((fgc - fgu) + (ftc - ftu) + to);
        return Math.round(index * 100.0) / 100.0;
    }

    /** Returns value for a metric (MIN, 2PÚ, PTS, 2P%, INDEX, etc.) or null. */
    public Double getMetricValue(String metric) {
        return switch (metric) {
            case "MIN" -> min != null ? min.doubleValue() : null;
            case "2PÚ" -> twoPMade != null ? twoPMade.doubleValue() : null;
            case "2PC" -> twoPAttempts != null ? twoPAttempts.doubleValue() : null;
            case "3PÚ" -> threePMade != null ? threePMade.doubleValue() : null;
            case "3PC" -> threePAttempts != null ? threePAttempts.doubleValue() : null;
            case "FGÚ" -> fgMade != null ? fgMade.doubleValue() : null;
            case "FGC" -> fgAttempts != null ? fgAttempts.doubleValue() : null;
            case "FTÚ" -> ftMade != null ? ftMade.doubleValue() : null;
            case "FTC" -> ftAttempts != null ? ftAttempts.doubleValue() : null;
            case "PTS" -> pts != null ? pts.doubleValue() : null;
            case "F+" -> foulsPlus != null ? foulsPlus.doubleValue() : null;
            case "F-" -> foulsMinus != null ? foulsMinus.doubleValue() : null;
            case "OFF" -> offReb != null ? offReb.doubleValue() : null;
            case "DEF" -> defReb != null ? defReb.doubleValue() : null;
            case "STL" -> stl != null ? stl.doubleValue() : null;
            case "AST" -> ast != null ? ast.doubleValue() : null;
            case "BLK" -> blk != null ? blk.doubleValue() : null;
            case "TO" -> turnovers != null ? turnovers.doubleValue() : null;
            case "INDEX" -> statIndex != null ? statIndex : calculateIndex();
            case "2P%" -> twoPAttempts != null && twoPAttempts > 0 && twoPMade != null
                ? (twoPMade.doubleValue() / twoPAttempts) * 100 : null;
            case "3P%" -> threePAttempts != null && threePAttempts > 0 && threePMade != null
                ? (threePMade.doubleValue() / threePAttempts) * 100 : null;
            case "FG%" -> fgAttempts != null && fgAttempts > 0 && fgMade != null
                ? (fgMade.doubleValue() / fgAttempts) * 100 : null;
            case "FT%" -> ftAttempts != null && ftAttempts > 0 && ftMade != null
                ? (ftMade.doubleValue() / ftAttempts) * 100 : null;
            default -> null;
        };
    }

    /** Match statistics only. Training data is in {@link TrainingStatistics}. */
    public enum StatType {
        GAME
    }
}
