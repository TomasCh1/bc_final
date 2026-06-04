package com.basketball.app.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Entity
@Table(name = "beh_stats")
@PrimaryKeyJoinColumn(name = "id")
@DiscriminatorValue("BEH")
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
public class BehStats extends TrainingStatistics {
    @Column(length = 20)
    private String time;

    @Column(precision = 10, scale = 2)
    private BigDecimal distance;
}
