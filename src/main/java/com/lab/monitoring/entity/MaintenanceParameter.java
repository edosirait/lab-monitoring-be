package com.lab.monitoring.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "maintenance_parameters")
public class MaintenanceParameter {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    // Relasi ke Instrument (banyak parameter milik satu instrument)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "instrument_id")
    private Instrument instrument;

    @Column(name = "parameter_name", nullable = false)
    private String parameterName;

    @Enumerated(EnumType.STRING)
    @Column(name = "type_input", nullable = false)
    private Enums.InputType typeInput;

    private String unit;

    @Column(name = "threshold_value")
    private BigDecimal thresholdValue;

    @Column(name = "threshold_operator", length = 5)
    private String thresholdOperator;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
