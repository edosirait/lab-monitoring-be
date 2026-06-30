package com.lab.monitoring.controller;

import com.lab.monitoring.dto.GlobalSummaryDto;
import com.lab.monitoring.entity.Enums;
import com.lab.monitoring.entity.Instrument;
import com.lab.monitoring.entity.MaintenanceParameter;
import com.lab.monitoring.entity.MaintenanceRecord;
import com.lab.monitoring.repository.IncidentReportRepository;
import com.lab.monitoring.repository.InstrumentRepository;
import com.lab.monitoring.repository.MaintenanceParameterRepository;
import com.lab.monitoring.repository.MaintenanceRecordRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;

@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class GlobalDashboardController {

    private final InstrumentRepository instrumentRepository;
    private final MaintenanceParameterRepository parameterRepository;
    private final MaintenanceRecordRepository recordRepository;
    private final IncidentReportRepository incidentReportRepository;

    @GetMapping("/global-summary")
    public ResponseEntity<GlobalSummaryDto> getGlobalSummary() {
        long totalInstruments = instrumentRepository.count();
        long openIncidents = incidentReportRepository.countByStatus("OPEN");

        int currentYear = LocalDate.now().getYear();
        int currentMonth = LocalDate.now().getMonthValue();
        LocalDate now = LocalDate.now();

        // Hitung sisa hari menuju akhir bulan berjalan (untuk aturan H-7)
        LocalDate lastDayOfMonth = now.withDayOfMonth(now.lengthOfMonth());
        long daysRemaining = ChronoUnit.DAYS.between(now, lastDayOfMonth);

        // 1. Tarik seluruh master parameter dan transaksi record tahun berjalan
        List<MaintenanceParameter> allParameters = parameterRepository.findAll();
        List<MaintenanceRecord> yearRecords = recordRepository.findAll();

        // Bangun indeks pencarian cepat: parameterId -> monthValue -> MaintenanceRecord
        Map<Integer, Map<Integer, MaintenanceRecord>> recordMap = new HashMap<>();
        for (MaintenanceRecord rec : yearRecords) {
            if (rec.getParameter() == null || rec.getRecordMonth() == null || rec.getRecordMonth().getYear() != currentYear) continue;
            int paramId = rec.getParameter().getId();
            int mVal = rec.getRecordMonth().getMonthValue();
            recordMap.computeIfAbsent(paramId, k -> new HashMap<>()).put(mVal, rec);
        }

        // Variabel penampung metrik agregasi
        long totalChecklists = 0;
        long completedChecklists = 0;
        Set<Integer> criticalInstrumentIds = new HashSet<>();
        List<Map<String, Object>> criticalDetailList = new ArrayList<>();

        long goodCount = 0;
        long warningCount = 0;
        long dangerCount = 0;

        // Inisialisasi struktur tren bulanan 1-12
        Map<Integer, Long> monthlyAlerts = new HashMap<>();
        Map<Integer, Long> monthlyTotalChk = new HashMap<>();
        Map<Integer, Long> monthlyDoneChk = new HashMap<>();
        for (int m = 1; m <= 12; m++) {
            monthlyAlerts.put(m, 0L);
            monthlyTotalChk.put(m, 0L);
            monthlyDoneChk.put(m, 0L);
        }

        // 2. JELAJAHI MATRIKS DINAMIS KESELURUHAN PARAMETER & BULAN
        for (MaintenanceParameter param : allParameters) {
            int paramId = param.getId();
            Instrument inst = param.getInstrument();
            if (inst == null) continue;

            Map<Integer, MaintenanceRecord> paramMonths = recordMap.getOrDefault(paramId, Collections.emptyMap());

            for (int m = 1; m <= 12; m++) {
                // Jangan hitung bulan masa depan yang belum dilalui
                if (m > currentMonth) {
                    goodCount++;
                    continue;
                }

                MaintenanceRecord record = paramMonths.get(m);
                String evaluatedCondition = "GOOD";
                Object displayValue = "Belum Diisi";

                if (Enums.InputType.CHECKLIST.equals(param.getTypeInput())) {
                    // --- LOGIKA DINAMIS EVALUASI TUGAS CHECKLIST ---
                    monthlyTotalChk.put(m, monthlyTotalChk.get(m) + 1);
                    if (m == currentMonth) totalChecklists++;

                    if (record != null && Boolean.TRUE.equals(record.getChecklistStatus())) {
                        // Tugas berhasil diselesaikan
                        completedChecklists++;
                        monthlyDoneChk.put(m, monthlyDoneChk.get(m) + 1);
                        evaluatedCondition = "GOOD";
                        displayValue = "Dilakukan";
                    } else {
                        // Tugas tidak dicentang / belum diisi sama sekali
                        displayValue = "Tidak Dilakukan";
                        if (m < currentMonth) {
                            // Kelalaian bulan masa lalu langsung DANGER
                            evaluatedCondition = "DANGER";
                        } else if (m == currentMonth && daysRemaining <= 7) {
                            // Masuk masa kritis tenggat waktu H-7 bulan ini
                            evaluatedCondition = "DANGER";
                        } else {
                            evaluatedCondition = "GOOD"; // Masih aman jauh dari H-7
                        }
                    }
                }
                else if (Enums.InputType.NUMERIC.equals(param.getTypeInput())) {
                    // --- 🔥 ATURAN DINAMIS NUMERIC BARU ---
                    if (record != null && record.getNumericValue() != null) {
                        evaluatedCondition = record.getItemCondition() != null ? record.getItemCondition().name() : "GOOD";
                        displayValue = record.getNumericValue();
                    } else {
                        // Sesuai request: Jika angka kosong atau 0 belum ada aktivitas, dianggap AMAN/NEUTRAL (Tidak Kena Penalty)
                        evaluatedCondition = "GOOD";
                        displayValue = "-";
                    }
                }

                // Masukkan hasil evaluasi ke dalam counter dashboard
                if ("DANGER".equals(evaluatedCondition)) {
                    dangerCount++;
                    monthlyAlerts.put(m, monthlyAlerts.get(m) + 1);
                    criticalInstrumentIds.add(inst.getId());

                    // Rekam detail masalah untuk Modal Atensi
                    Map<String, Object> anomaly = new HashMap<>();
                    anomaly.put("instrumentId", inst.getId());
                    anomaly.put("instrumentName", inst.getName());
                    anomaly.put("location", inst.getLocation());
                    anomaly.put("parameterName", param.getParameterName());
                    anomaly.put("condition", "DANGER");
                    anomaly.put("month", m);
                    anomaly.put("value", displayValue);
                    criticalDetailList.add(anomaly);
                } else if ("WARNING".equals(evaluatedCondition)) {
                    warningCount++;
                    monthlyAlerts.put(m, monthlyAlerts.get(m) + 1);
                    criticalInstrumentIds.add(inst.getId());
                } else {
                    goodCount++;
                }
            }
        }

        // Kalkulasi nilai persentase kepatuhan kerja bulan berjalan
        double complianceRate = totalChecklists > 0
                ? Math.round(((double) completedChecklists / totalChecklists) * 100.0)
                : 100.0;

        Map<String, Long> distribution = new HashMap<>();
        distribution.put("GOOD", goodCount);
        distribution.put("WARNING", warningCount);
        distribution.put("DANGER", dangerCount);

        // Susun daftar tren bulanan kumulatif sepanjang tahun
        List<Map<String, Object>> monthlyTrendList = new ArrayList<>();
        for (int m = 1; m <= 12; m++) {
            long totChk = monthlyTotalChk.get(m);
            long donChk = monthlyDoneChk.get(m);
            double compPercent = totChk > 0 ? Math.round(((double) donChk / totChk) * 100.0) : 100.0;

            Map<String, Object> monthData = new HashMap<>();
            monthData.put("month", m);
            monthData.put("compliance", compPercent);
            monthData.put("alerts", monthlyAlerts.get(m));
            monthlyTrendList.add(monthData);
        }

        GlobalSummaryDto summary = new GlobalSummaryDto(
                totalInstruments,
                criticalInstrumentIds.size(), // Jumlah fisik alat unik yang bermasalah
                complianceRate,
                openIncidents,
                distribution,
                monthlyTrendList,
                criticalDetailList
        );

        return ResponseEntity.ok(summary);
    }
}
