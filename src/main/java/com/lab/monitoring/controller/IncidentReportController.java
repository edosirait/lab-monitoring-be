package com.lab.monitoring.controller;

import com.lab.monitoring.entity.IncidentReport;
import com.lab.monitoring.repository.IncidentReportRepository;
import com.lab.monitoring.repository.InstrumentRepository;
import com.lab.monitoring.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

@RestController
@RequestMapping("/api/incidents")
@RequiredArgsConstructor
public class IncidentReportController {

    private final IncidentReportRepository incidentReportRepository;
    private final InstrumentRepository instrumentRepository;
    private final UserRepository userRepository;

    /**
     * 🔥 BARU: Endpoint memuat SELURUH data logbook (Opsi ALL luar)
     * URL: GET /api/incidents
     */
    @GetMapping
    public ResponseEntity<List<IncidentReport>> getAll() {
        return ResponseEntity.ok(incidentReportRepository.findAllByOrderByReportDateDesc());
    }

    /**
     * ⚡ UPDATE: Menangani rute instrumen spesifik maupun kategori OTHER (-1)
     * URL: GET /api/incidents/instrument/{instrumentId}
     */
    @GetMapping("/instrument/{instrumentId}")
    public ResponseEntity<List<IncidentReport>> getByInstrument(@PathVariable Integer instrumentId) {
        // Jika parameter bernilai -1, panggil fungsi pencarian data fasilitas umum (OTHER)
        if (instrumentId != null && instrumentId == -1) {
            return ResponseEntity.ok(incidentReportRepository.findByInstrumentIsNullOrderByReportDateDesc());
        }
        return ResponseEntity.ok(incidentReportRepository.findByInstrumentIdOrderByReportDateDesc(instrumentId));
    }

    /**
     * ⚡ UPDATE: Mengamankan proses simpan data baru untuk tipe instrumen maupun OTHER (-1)
     * URL: POST /api/incidents
     */
    @PostMapping
    public ResponseEntity<?> createIncident(@RequestBody Map<String, Object> payload) {
        IncidentReport report = new IncidentReport();

        // 🔥 PERBAIKAN: Gunakan java.lang.Number agar proses casting ID -1 dari JSON aman 100%
        if (payload.get("instrumentId") != null) {
            Integer instrumentId = ((Number) payload.get("instrumentId")).intValue();

            if (instrumentId == -1) {
                report.setInstrument(null); // Set NULL sebagai tanda logbook kategori OTHER
            } else {
                report.setInstrument(instrumentRepository.findById(instrumentId)
                        .orElseThrow(() -> new NoSuchElementException("Instrumen tidak ditemukan")));
            }
        } else {
            report.setInstrument(null);
        }

        // Ambil data user pelapor yang sedang aktif login di FE
        if (payload.get("reportedByUserId") != null) {
            Integer reporterId = ((Number) payload.get("reportedByUserId")).intValue();
            report.setReportedByUser(userRepository.findById(reporterId).orElse(null));
        }

        report.setTitle(payload.get("title").toString());
        report.setDescription(payload.get("description") != null ? payload.get("description").toString() : "");
        report.setIssueDescription(payload.get("issueDescription") != null ? payload.get("issueDescription").toString() : "");
        report.setResolutionNotes(payload.get("resolutionNotes") != null ? payload.get("resolutionNotes").toString() : "");

        report.setStatus(payload.get("status") != null ? payload.get("status").toString() : "OPEN");
        report.setReportedAt(LocalDateTime.now());
        report.setReportDate(LocalDateTime.now());

        if ("CLOSED".equals(report.getStatus())) {
            report.setResolvedAt(LocalDateTime.now());
            report.setUser(report.getReportedByUser());
        }

        return ResponseEntity.ok(incidentReportRepository.save(report));
    }

    @PutMapping("/{id}/resolve")
    public ResponseEntity<?> resolveIncident(@PathVariable Integer id, @RequestBody Map<String, Object> payload) {
        IncidentReport report = incidentReportRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Data logbook tidak ditemukan"));

        report.setResolutionNotes(payload.get("resolutionNotes").toString());
        report.setStatus("CLOSED");
        report.setResolvedAt(LocalDateTime.now());

        if (payload.get("userId") != null) {
            Integer userId = ((Number) payload.get("userId")).intValue();
            report.setUser(userRepository.findById(userId).orElse(null));
        }

        return ResponseEntity.ok(incidentReportRepository.save(report));
    }
}
