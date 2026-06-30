package com.lab.monitoring.dto;

import lombok.Data;

@Data
public class RecordSubmitDTO {
    private Integer parameterId;
    private Integer userId; // ID staf yang sedang login/menginput
    private int year;       // Tahun (misal: 2026)
    private int month;      // Bulan ke- (1 sampai 12)
    private String value;   // Nilai yang diinput (bisa "true" atau angka "3816")
    private String notes;
}
