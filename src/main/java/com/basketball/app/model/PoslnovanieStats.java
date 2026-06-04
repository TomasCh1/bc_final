package com.basketball.app.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Entity
@Table(name = "poslnovanie_stats")
@PrimaryKeyJoinColumn(name = "id")
@DiscriminatorValue("POSLNOVANIE")
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
public class PoslnovanieStats extends TrainingStatistics {
    @Column(precision = 10, scale = 2)
    private BigDecimal weight;

    private Integer repetitions;
}
