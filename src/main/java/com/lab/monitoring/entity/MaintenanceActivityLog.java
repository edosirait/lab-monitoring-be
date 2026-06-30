package com.lab.monitoring.entity;

import lombok.Data;
import lombok.NoArgsConstructor;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@Entity
@Table(name = "maintenance_activity_logs")
public class MaintenanceActivityLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parameter_id")
    private MaintenanceParameter parameter;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(name = "log_date")
    private LocalDateTime logDate;

    @Column(name = "action_type")
    private String actionType; // Contoh: "RESET_TO_ZERO", "CHECKLIST_DONE"

    @Column(name = "previous_value")
    private BigDecimal previousValue; // Menyimpan nilai Gambar 1 sebelum di-reset (misal: 2500)

    @Column(name = "new_value")
    private BigDecimal newValue; // Menyimpan nilai setelah perbaikan (misal: 0)

    @Column(columnDefinition = "TEXT")
    private String notes; // Catatan otentik dari staf/SPV

    @Column(name = "record_month")
    private java.time.LocalDate recordMonth;
}
