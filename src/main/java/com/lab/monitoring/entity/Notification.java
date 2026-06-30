package com.lab.monitoring.entity;

import lombok.Data;
import lombok.NoArgsConstructor;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@Entity
@Table(name = "notifications")
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user; // Target penerima notifikasi (misal: para Supervisor/Manager)

    @Column(nullable = false)
    private String title; // Judul alarm (Contoh: "⚠️ Peringatan Kritis Alat")

    @Column(nullable = false, columnDefinition = "TEXT")
    private String message; // Detail isi pesan peringatan

    @Column(name = "is_read", nullable = false)
    private boolean read = false; // Status apakah Supervisor sudah membaca alarm ini

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
}
