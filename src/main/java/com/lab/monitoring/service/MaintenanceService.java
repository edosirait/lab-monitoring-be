package com.lab.monitoring.service;

import com.lab.monitoring.dto.ChartDataDTO;
import com.lab.monitoring.dto.DataGridRowDTO;
import com.lab.monitoring.dto.PageMetaDTO;
import com.lab.monitoring.dto.PagedResponseDTO;
import com.lab.monitoring.dto.RecordSubmitDTO;
import com.lab.monitoring.entity.Enums;
import com.lab.monitoring.entity.MaintenanceParameter;
import com.lab.monitoring.entity.MaintenanceRecord;
import com.lab.monitoring.entity.User;
import com.lab.monitoring.repository.MaintenanceActivityLogRepository;
import com.lab.monitoring.repository.MaintenanceParameterRepository;
import com.lab.monitoring.repository.MaintenanceRecordRepository;
import com.lab.monitoring.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Service
public class MaintenanceService {

    private final MaintenanceParameterRepository parameterRepository;
    private final MaintenanceRecordRepository recordRepository;
    private final UserRepository userRepository;
    private final MaintenanceActivityLogRepository activityLogRepository;

    public MaintenanceService(MaintenanceParameterRepository parameterRepository,
                              MaintenanceRecordRepository recordRepository,
                              UserRepository userRepository,
                              MaintenanceActivityLogRepository activityLogRepository) {
        this.parameterRepository = parameterRepository;
        this.recordRepository = recordRepository;
        this.userRepository = userRepository;
        this.activityLogRepository = activityLogRepository;
    }

    @Transactional(readOnly = true)
    public PagedResponseDTO<DataGridRowDTO> getGridData(Integer instrumentId,
                                                        int year,
                                                        Integer page,
                                                        Integer size,
                                                        Integer offset,
                                                        Integer limit) {
        List<MaintenanceParameter> parameters = parameterRepository.findByInstrumentId(instrumentId);

        // --- Pagination normalization ---
        int resolvedSize = (size != null && size > 0) ? size : 20;
        int resolvedPage = (page != null && page >= 0) ? page : 0;
        int resolvedOffset;
        int resolvedLimit;

        if (offset != null || limit != null) {
            resolvedOffset = Math.max(0, offset != null ? offset : 0);
            resolvedLimit = Math.max(1, limit != null ? limit : resolvedSize);
            resolvedPage = resolvedOffset / resolvedLimit;
            resolvedSize = resolvedLimit;
        } else {
            resolvedOffset = resolvedPage * resolvedSize;
            resolvedLimit = resolvedSize;
        }

        long length = parameters.size();
        int from = Math.min(resolvedOffset, parameters.size());
        int to = Math.min(resolvedOffset + resolvedLimit, parameters.size());
        List<MaintenanceParameter> pagedParams = parameters.subList(from, to);

        // --- Ambil semua record instrument+year sekali query ---
        List<MaintenanceRecord> allRecords = recordRepository.findByInstrumentIdAndYear(instrumentId, year);

        // Index: parameterId -> month -> record
        Map<Integer, Map<Integer, MaintenanceRecord>> byParamMonth = new HashMap<>();
        for (MaintenanceRecord r : allRecords) {
            if (r.getParameter() == null || r.getParameter().getId() == null || r.getRecordMonth() == null) continue;
            int paramId = r.getParameter().getId();
            int month = r.getRecordMonth().getMonthValue();
            byParamMonth.computeIfAbsent(paramId, k -> new HashMap<>()).put(month, r);
        }

        // Variabel penunjuk waktu riil server saat ini (Tahun 2026)
        LocalDate now = LocalDate.now();
        int currentYearReal = now.getYear();
        int currentMonthReal = now.getMonthValue();

        LocalDate lastDayOfMonth = now.withDayOfMonth(now.lengthOfMonth());
        long daysRemaining = java.time.temporal.ChronoUnit.DAYS.between(now, lastDayOfMonth);

        List<DataGridRowDTO> rows = new ArrayList<>();
        for (MaintenanceParameter p : pagedParams) {
            DataGridRowDTO row = new DataGridRowDTO();
            row.setParameterId(p.getId());
            row.setMaintenance(p.getParameterName());
            row.setTypeInput(p.getTypeInput() != null ? p.getTypeInput().name() : null);
            row.setSyarat(buildSyarat(p));

            Map<Integer, MaintenanceRecord> monthMap = byParamMonth.getOrDefault(p.getId(), Collections.emptyMap());
            // Cari blok ini di dalam method getGridData pada MaintenanceService.java Anda:
            for (int m = 1; m <= 12; m++) {
                MaintenanceRecord r = monthMap.get(m);

                // 🔥 UPDATE: LOGIKA TERUKUR UNTUK SEL YANG MASIH KOSONG DI DATABASE
                if (r == null) {
                    if (Enums.InputType.CHECKLIST.equals(p.getTypeInput())) {
                        // Checklist kosong di masa lalu atau H-7 wajib menyala merah DANGER
                        if (year < currentYearReal || (year == currentYearReal && m < currentMonthReal)) {
                            row.getConditions().put(m, Enums.ComponentCondition.DANGER.name());
                        } else if (year == currentYearReal && m == currentMonthReal && daysRemaining <= 7) {
                            row.getConditions().put(m, Enums.ComponentCondition.DANGER.name());
                        } else {
                            row.getConditions().put(m, Enums.ComponentCondition.GOOD.name()); // Status normal untuk bulan berjalan awal
                        }
                    } else {
                        // 🔥 BARU: Numeric kosong/belum diisi = NORMAL (Bebas dari status Danger kelalaian)
                        row.getConditions().put(m, Enums.ComponentCondition.GOOD.name());
                    }
                    continue;
                }

                // Jika data transaksi ada di database, tampilkan nilai asli apa adanya
                row.getValues().put(m, toDisplayValue(p.getTypeInput(), r));
                row.getConditions().put(m, r.getItemCondition() != null ? r.getItemCondition().name() : Enums.ComponentCondition.GOOD.name());
            }

            rows.add(row);
        }

        PageMetaDTO meta = new PageMetaDTO(length, resolvedPage, resolvedSize, resolvedOffset, resolvedLimit);
        return new PagedResponseDTO<>(meta, rows);
    }

    @Transactional
    public MaintenanceRecord saveRecord(RecordSubmitDTO dto) {
        if (dto.getParameterId() == null) {
            throw new IllegalArgumentException("parameterId wajib diisi");
        }
        if (dto.getYear() <= 0) {
            throw new IllegalArgumentException("year wajib diisi");
        }
        if (dto.getMonth() < 1 || dto.getMonth() > 12) {
            throw new IllegalArgumentException("month harus 1-12");
        }

        MaintenanceParameter parameter = parameterRepository.findById(dto.getParameterId())
                .orElseThrow(() -> new NoSuchElementException("Parameter tidak ditemukan"));

        User user = null;
        if (dto.getUserId() != null) {
            user = userRepository.findById(dto.getUserId())
                    .orElseThrow(() -> new NoSuchElementException("User tidak ditemukan"));
        }

        LocalDate recordMonth = LocalDate.of(dto.getYear(), dto.getMonth(), 1);

        // True upsert sesuai unique constraint (parameter_id, record_month)
        MaintenanceRecord record = recordRepository.findByParameterIdAndRecordMonth(dto.getParameterId(), recordMonth)
                .orElseGet(MaintenanceRecord::new);

        record.setParameter(parameter);
        record.setUser(user);
        record.setRecordMonth(recordMonth);
        record.setNotes(dto.getNotes());

        // =========================================================================
        // KONDISI 1: INPUT BERJENIS CHECKLIST
        // =========================================================================
        if (Enums.InputType.CHECKLIST.equals(parameter.getTypeInput())) {
            boolean checked = parseBooleanStrict(dto.getValue());
            record.setChecklistStatus(checked);
            record.setNumericValue(null);
            // checklist: false => DANGER, true => GOOD
            record.setItemCondition(checked ? Enums.ComponentCondition.GOOD : Enums.ComponentCondition.DANGER);
        }
        // =========================================================================
        // KONDISI 2: INPUT BERJENIS NUMERIC (SUDAH DIPERBAIKI LINTAS BULAN)
        // =========================================================================
        else if (Enums.InputType.NUMERIC.equals(parameter.getTypeInput())) {
            BigDecimal numeric = parseBigDecimalStrict(dto.getValue());

            // 🔥 LOGIKA AUTO-SNAPSHOT HISTORI LINTAS BULAN
            // Jika angka BARU yang diketik staf adalah 0 (Artinya alat telah diperbaiki/suku cadang diganti)
            if (numeric != null && numeric.compareTo(BigDecimal.ZERO) == 0) {

                // Ambil semua riwayat transaksi instrumen ini pada tahun berjalan, lalu cari angka puncak di bulan-bulan sebelumnya
                Optional<MaintenanceRecord> latestPastRecord = recordRepository.findByInstrumentIdAndYear(parameter.getInstrument().getId(), dto.getYear())
                        .stream()
                        .filter(r -> r.getParameter().getId().equals(parameter.getId())) // Filter parameter yang sama
                        .filter(r -> r.getRecordMonth() != null && r.getRecordMonth().getMonthValue() < dto.getMonth()) // Hanya bulan-bulan sebelumnya
                        .filter(r -> r.getNumericValue() != null && r.getNumericValue().compareTo(BigDecimal.ZERO) > 0) // Angka pemakaian di atas 0
                        .max(Comparator.comparing(r -> r.getRecordMonth().getMonthValue())); // Ambil bulan terdekat/terakhir sebelum bulan ini

                if (latestPastRecord.isPresent()) {
                    BigDecimal previousPeakValue = latestPastRecord.get().getNumericValue(); // Menangkap nilai akumulasi terakhir (misal: 11000)
                    BigDecimal threshold = parameter.getThresholdValue();

                    // Log perbaikan hanya diciptakan jika nilai akumulasi bulan lalu memang sudah melanggar batas threshold
                    if (threshold != null && previousPeakValue.compareTo(threshold) > 0) {
                        com.lab.monitoring.entity.MaintenanceActivityLog historyLog = new com.lab.monitoring.entity.MaintenanceActivityLog();
                        historyLog.setParameter(parameter);
                        historyLog.setUser(user);
                        historyLog.setLogDate(java.time.LocalDateTime.now());
                        historyLog.setRecordMonth(recordMonth); // Dikunci di bulan Juni (waktu eksekusi reset angka 0)
                        historyLog.setActionType("RESET_TO_ZERO");
                        historyLog.setPreviousValue(previousPeakValue); // Angka puncak lama terabadikan aman
                        historyLog.setNewValue(BigDecimal.ZERO);

                        // Ambil nama bulan teks secara dinamis (Locale Indonesia)
                        String namaBulanTeks = recordMonth.getMonth().getDisplayName(
                                java.time.format.TextStyle.FULL, new java.util.Locale("id", "ID"));

                        historyLog.setNotes(dto.getNotes() != null && !dto.getNotes().isBlank()
                                ? dto.getNotes()
                                : "Pemeliharaan/Penggantian suku cadang selesai dilakukan pada bulan " + namaBulanTeks + ". Masa pakai puncak komponen sebelumnya berhasil diabadikan: "
                                + stripTrailingZeros(previousPeakValue) + " " + (parameter.getUnit() != null ? parameter.getUnit() : "jam"));

                        // Simpan objek log ke database (Akan muncul di grafik tab History Manager)
                        activityLogRepository.save(historyLog);
                    }
                }
            }

            // Terapkan nilai operasional baru (angka 0) ke dalam sel bulan berjalan
            record.setNumericValue(numeric);
            record.setChecklistStatus(null);
            record.setItemCondition(evaluateNumericCondition(numeric, parameter.getThresholdOperator(), parameter.getThresholdValue()));
        } else {
            throw new IllegalStateException("InputType tidak dikenali: " + parameter.getTypeInput());
        }

        return recordRepository.save(record);
    }

    private String buildSyarat(MaintenanceParameter p) {
        if (p.getTypeInput() == null) return "";
        if (Enums.InputType.CHECKLIST.equals(p.getTypeInput())) return "Dilakukan";
        if (p.getThresholdValue() != null && p.getThresholdOperator() != null && !p.getThresholdOperator().isBlank()) {
            String unit = (p.getUnit() != null && !p.getUnit().isBlank()) ? (" " + p.getUnit()) : "";
            return p.getThresholdOperator() + " " + stripTrailingZeros(p.getThresholdValue()) + unit;
        }
        return "";
    }

    private String toDisplayValue(Enums.InputType type, MaintenanceRecord r) {
        if (Enums.InputType.CHECKLIST.equals(type)) {
            return Boolean.TRUE.equals(r.getChecklistStatus()) ? "Dilakukan" : "Tidak";
        }
        if (Enums.InputType.NUMERIC.equals(type)) {
            return r.getNumericValue() != null ? stripTrailingZeros(r.getNumericValue()) : null;
        }
        return null;
    }

    private String stripTrailingZeros(BigDecimal v) {
        if (v == null) return null;
        BigDecimal normalized = v.stripTrailingZeros();
        if (normalized.scale() < 0) {
            normalized = normalized.setScale(0, RoundingMode.UNNECESSARY);
        }
        return normalized.toPlainString();
    }

    private boolean parseBooleanStrict(String raw) {
        if (raw == null) throw new IllegalArgumentException("value wajib diisi");
        String s = raw.trim().toLowerCase(Locale.ROOT);
        if (s.equals("true") || s.equals("1") || s.equals("yes") || s.equals("ya")) return true;
        if (s.equals("false") || s.equals("0") || s.equals("no") || s.equals("tidak")) return false;
        throw new IllegalArgumentException("value checklist harus boolean (true/false)");
    }

    private BigDecimal parseBigDecimalStrict(String raw) {
        if (raw == null || raw.isBlank()) throw new IllegalArgumentException("value numeric wajib diisi");
        try {
            return new BigDecimal(raw.trim());
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("value numeric tidak valid: " + raw);
        }
    }

    /**
     * 🔥 UPDATE 3: Evaluasi kondisi numerik yang presisi
     */
    private Enums.ComponentCondition evaluateNumericCondition(BigDecimal value, String op, BigDecimal threshold) {
        if (threshold == null || op == null || op.isBlank()) {
            return Enums.ComponentCondition.GOOD;
        }

        String operator = op.trim();
        BigDecimal twoX = threshold.multiply(new BigDecimal("2"));
        int cmp = value.compareTo(threshold);

        switch (operator) {
            case ">":
            case ">=":
                if (cmp > 0) return value.compareTo(twoX) >= 0 ? Enums.ComponentCondition.DANGER : Enums.ComponentCondition.WARNING;
                return Enums.ComponentCondition.GOOD;

            case "<":
            case "<=":
                if (cmp > 0) {
                    return value.compareTo(twoX) >= 0 ? Enums.ComponentCondition.DANGER : Enums.ComponentCondition.WARNING;
                }
                return Enums.ComponentCondition.GOOD;

            case "==":
            case "=":
                return cmp == 0 ? Enums.ComponentCondition.GOOD : Enums.ComponentCondition.WARNING;
            default:
                return Enums.ComponentCondition.GOOD;
        }
    }

    @Transactional(readOnly = true)
    public List<ChartDataDTO> getChecklistChart(Integer parameterId, int year) {
        List<Object[]> raw = recordRepository.getChecklistFrequencySummary(parameterId, year);
        Map<Integer, Long> monthToCount = new HashMap<>();
        if (raw != null) {
            for (Object[] row : raw) {
                if (row == null || row.length < 2) continue;
                Number monthNum = (row[0] instanceof Number) ? (Number) row[0] : null;
                Number cntNum = (row[1] instanceof Number) ? (Number) row[1] : null;
                if (monthNum == null) continue;
                int month = monthNum.intValue();
                long cnt = cntNum != null ? cntNum.longValue() : 0L;
                monthToCount.put(month, cnt);
            }
        }
        List<ChartDataDTO> out = new ArrayList<>();
        for (int m = 1; m <= 12; m++) {
            out.add(new ChartDataDTO(m, monthToCount.getOrDefault(m, 0L), null, null, null));
        }
        return out;
    }

    @Transactional(readOnly = true)
    public List<ChartDataDTO> getNumericChart(Integer parameterId, int year) {
        List<ChartDataDTO> dbRecords = recordRepository.getNumericTrendLog(parameterId, year);
        Map<Integer, ChartDataDTO> monthToDataMap = new HashMap<>();
        if (dbRecords != null) {
            for (ChartDataDTO record : dbRecords) {
                if (record != null && record.getMonth() != null) {
                    monthToDataMap.put(record.getMonth(), record);
                }
            }
        }
        List<ChartDataDTO> out = new ArrayList<>();
        for (int m = 1; m <= 12; m++) {
            if (monthToDataMap.containsKey(m)) {
                out.add(monthToDataMap.get(m));
            } else {
                out.add(new ChartDataDTO(m, null, null, null, null));
            }
        }
        return out;
    }
}
