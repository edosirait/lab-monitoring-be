package com.lab.monitoring.config;

import com.lab.monitoring.entity.*;
import com.lab.monitoring.repository.MaintenanceParameterRepository;
import com.lab.monitoring.repository.MaintenanceRecordRepository;
import com.lab.monitoring.repository.NotificationRepository;
import com.lab.monitoring.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Component
@RequiredArgsConstructor
public class MaintenanceScheduler {

    private final MaintenanceParameterRepository parameterRepository;
    private final MaintenanceRecordRepository recordRepository;
    private final UserRepository userRepository;
    private final NotificationRepository notificationRepository;

    private static final String[] NAMA_BULAN = {
            "", "Januari", "Februari", "Maret", "April", "Mei", "Juni",
            "Juli", "Agustus", "September", "Oktober", "November", "Desember"
    };

    @Scheduled(cron = "0 0 8 * * ?")
    @Transactional
    public void checkEquipmentThresholds() {
        System.out.println("⏰ [SCHEDULER] Memulai pemindaian otomatis sinkronisasi: " + LocalDateTime.now());

        LocalDate now = LocalDate.now();
        int currentYear = now.getYear();
        int currentMonth = now.getMonthValue();

        LocalDate lastDayOfMonth = now.withDayOfMonth(now.lengthOfMonth());
        long daysRemaining = ChronoUnit.DAYS.between(now, lastDayOfMonth);

        List<MaintenanceParameter> allParameters = parameterRepository.findAll();
        List<User> supervisors = userRepository.findByRole(Enums.UserRole.SUPERVISOR);

        if (supervisors.isEmpty()) return;

        List<MaintenanceRecord> yearRecords = recordRepository.findAll();

        Map<Integer, Map<Integer, MaintenanceRecord>> recordMap = new HashMap<>();
        for (MaintenanceRecord rec : yearRecords) {
            if (rec.getParameter() == null || rec.getRecordMonth() == null || rec.getRecordMonth().getYear() != currentYear) continue;
            int pId = rec.getParameter().getId();
            int mVal = rec.getRecordMonth().getMonthValue();
            recordMap.computeIfAbsent(pId, k -> new HashMap<>()).put(mVal, rec);
        }

        for (MaintenanceParameter param : allParameters) {
            Instrument inst = param.getInstrument();
            if (inst == null) continue;

            Map<Integer, MaintenanceRecord> paramMonths = recordMap.getOrDefault(param.getId(), Collections.emptyMap());

            for (int m = 1; m <= currentMonth; m++) {
                MaintenanceRecord record = paramMonths.get(m);
                boolean isDanger = false;
                String reasonMessage = "";
                String namaBulanTeks = NAMA_BULAN[m];

                // 1. EVALUASI TYPE INPUT CHECKLIST
                if (Enums.InputType.CHECKLIST.equals(param.getTypeInput())) {
                    boolean hasDone = (record != null && Boolean.TRUE.equals(record.getChecklistStatus()));
                    if (!hasDone) {
                        if (m < currentMonth) {
                            isDanger = true;
                            reasonMessage = String.format("mengalami kelalaian karena BELUM DIKERJAKAN pada periode bulan %s %d.", namaBulanTeks, currentYear);
                        }
                        // 😉 PERUBAHAN 2: Aktif eksklusif hanya di hari H-7 akhir bulan
                        else if (m == currentMonth && daysRemaining == 7) {
                            isDanger = true;
                            reasonMessage = String.format("memasuki masa kritis TEPAT H-7 sebelum akhir bulan %s dan belum diberi tindakan checklist.", namaBulanTeks);
                        }
                    }
                }
                // 2. EVALUASI TYPE INPUT NUMERIC
                else if (Enums.InputType.NUMERIC.equals(param.getTypeInput())) {
                    if (record != null && record.getNumericValue() != null && record.getNumericValue().compareTo(BigDecimal.ZERO) > 0) {
                        BigDecimal val = record.getNumericValue();
                        BigDecimal limit = param.getThresholdValue();
                        String op = param.getThresholdOperator();

                        if (limit != null && op != null && !op.isBlank()) {
                            String cleanOp = op.trim();
                            int cmp = val.compareTo(limit);

                            if (">".equals(cleanOp) || ">=".equals(cleanOp) || "<".equals(cleanOp) || "<=".equals(cleanOp)) {
                                if (cmp > 0) isDanger = true;
                            } else if ("=".equals(cleanOp) || "==".equals(cleanOp)) {
                                if (cmp != 0) isDanger = true;
                            }

                            if (isDanger) {
                                reasonMessage = String.format("pada periode %s %d bernilai %s, telah melanggar batas aman prasyarat (%s %s).",
                                        namaBulanTeks, currentYear, val.stripTrailingZeros().toPlainString(), cleanOp, limit.stripTrailingZeros().toPlainString());
                            }
                        }
                    }
                }

                // Pola pencocokan judul unik alarm
                String alertTitle = String.format("🚨 ALARM KRITIS: [%s] %s - %s %d",
                        inst.getName(), param.getParameterName(), namaBulanTeks, currentYear);

                // ----------------=========================================================
                // ALUR A: JIKA KONDISI TERDETEKSI BAHAYA (DANGER) -> DISPATCH / KIRIM NOTIFIKASI
                // ------------------------------------------------=========================
                if (isDanger) {
                    for (User supervisor : supervisors) {
                        boolean alreadyAlerted = notificationRepository.findAll().stream()
                                .anyMatch(n -> n.getUser().getId().equals(supervisor.getId())
                                        && n.getTitle().equals(alertTitle)
                                        && !n.isRead());

                        if (alreadyAlerted) continue;

                        Notification alert = new Notification();
                        alert.setUser(supervisor);
                        alert.setTitle(alertTitle);
                        alert.setMessage(String.format(
                                "Instrumen '%s' [%s] membutuhkan atensi segera! Parameter '%s' %s Harap lakukan inspeksi lapangan.",
                                inst.getName(), inst.getLocation(), param.getParameterName(), reasonMessage
                        ));
                        alert.setRead(false);
                        alert.setCreatedAt(LocalDateTime.now());

                        notificationRepository.save(alert);
                    }
                }
                // ----------------=========================================================
                // 🔥 ALUR B: JIKA SEKARANG SUDAH AMAN (GOOD) -> AUTO-CLEANUP NOTIFIKASI LAMA
                // ------------------------------------------------=========================
                else {
                    // Ambil semua notifikasi unread lama yang memiliki judul ini di database
                    List<Notification> staleNotifications = notificationRepository.findAll().stream()
                            .filter(n -> n.getTitle().equals(alertTitle) && !n.isRead())
                            .toList();

                    if (!staleNotifications.isEmpty()) {
                        for (Notification stale : staleNotifications) {
                            // Pilihan 1: Otomatis tandai dibaca (Read = true) agar hilang dari lonceng unread
                            stale.setRead(true);
                            notificationRepository.save(stale);

                            // Pilihan 2: Jika ingin benar-benar dihapus bersih dari database fisik:
                            // notificationRepository.delete(stale);
                        }
                        System.out.println("🧹 [AUTO-EVICTION] Sukses membersihkan alarm usang karena parameter sudah diperbaiki: " + alertTitle);
                    }
                }

            }
        }
    }
}
