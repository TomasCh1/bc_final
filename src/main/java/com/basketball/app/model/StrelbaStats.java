package com.basketball.app.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Entity
@Table(name = "strelba_stats")
@PrimaryKeyJoinColumn(name = "id")
@DiscriminatorValue("STRELBA")
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
public class StrelbaStats extends TrainingStatistics {
    @Column(name = "shots_attempted")
    private Integer shotsAttempted;

    @Column(name = "shots_made")
    private Integer shotsMade;

    @Column(name = "success_rate", precision = 5, scale = 2)
    private BigDecimal successRate;

    @Column(name = "court_spot", length = 255)
    private String courtSpot;
}
