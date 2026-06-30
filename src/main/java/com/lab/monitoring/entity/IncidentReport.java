package com.lab.monitoring.entity;

import lombok.Data;
import lombok.NoArgsConstructor;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@Entity
@Table(name = "incident_reports")
public class IncidentReport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    // 🔥 UPDATE: Mengubah nullable menjadi true agar bisa menampung logbook 'OTHER' (Fasilitas Umum)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "instrument_id", nullable = true)
    private Instrument instrument;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user; // PIC Supervisor/Manager yang menyelesaikan masalah (Kolom PIC di Excel)

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reported_by_user_id")
    private User reportedByUser; // Staf Lapangan yang melaporkan kejadian di awal

    @Column(name = "title", nullable = false, length = 200)
    private String title; // Nama Modul / Masalah utama

    @Column(name = "description", columnDefinition = "TEXT")
    private String description; // Penjelasan singkat keluhan awal

    @Column(name = "issue_description", columnDefinition = "TEXT")
    private String issueDescription; // Analisis mendalam / Root Cause (Akar Masalah)

    @Column(name = "resolution_notes", columnDefinition = "TEXT")
    private String resolutionNotes; // Action Taken / Solusi Langkah Perbaikan

    @Column(name = "status", nullable = false, length = 255)
    private String status = "OPEN"; // OPEN atau CLOSED

    @Column(name = "report_date")
    private LocalDateTime reportDate;

    @Column(name = "reported_at")
    private LocalDateTime reportedAt = LocalDateTime.now(); // Tanggal submit laporan masuk

    @Column(name = "resolved_at")
    private LocalDateTime resolvedAt;
}
