package com.lab.monitoring.dto; // Sesuaikan dengan folder package DTO Anda

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;

@Data
@NoArgsConstructor // Menambahkan constructor kosong bawaan lombok
public class ChartDataDTO {

    private Integer month;          // 1 - 12
    private Long activityCount;     // Untuk frekuensi checklist
    private BigDecimal value;       // Untuk log angka pemakaian berjalan (Numeric)
    private String itemCondition;   // 🔥 BARU: Status 'GOOD' / 'DANGER' saat itu
    private String notes;           // 🔥 BARU: Catatan/alasan perbaikan dari staf

    /**
     * CONSTRUCTOR KHUSUS REPOSITORY QUERY
     * Urutan parameter wajib 100% sama dengan urutan SELECT NEW pada JPQL Repository!
     */
    public ChartDataDTO(Integer month, Long activityCount, BigDecimal value, String itemCondition, String notes) {
        this.month = month;
        this.activityCount = activityCount;
        this.value = value;
        this.itemCondition = itemCondition;
        this.notes = notes;
    }
}
