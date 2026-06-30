package com.lab.monitoring.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "maintenance_records",
        uniqueConstraints = @UniqueConstraint(name = "uk_record_param_month", columnNames = {"parameter_id", "record_month"}))
public class MaintenanceRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    // Relasi ke Parameter
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parameter_id")
    private MaintenanceParameter parameter;

    // Relasi ke Staff yang menginput
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    /**
     * Bulan pencatatan (disimpan sebagai tanggal hari pertama bulan tsb).
     */
    @Column(name = "record_month")
    private LocalDate recordMonth;

    @Column(name = "checklist_status")
    private Boolean checklistStatus;

    @Column(name = "numeric_value")
    private BigDecimal numericValue;

    @Enumerated(EnumType.STRING)
    @Column(name = "item_condition")
    private Enums.ComponentCondition itemCondition = Enums.ComponentCondition.GOOD;

    private String notes;
}
