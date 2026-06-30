package com.lab.monitoring.dto;

import lombok.Data;

import java.util.HashMap;
import java.util.Map;

@Data
public class DataGridRowDTO {
    private Integer parameterId;
    private String maintenance; // Nama parameter (misal: "D2 Lamp")
    private String syarat;      // Syarat (misal: "> 2000 jam")
    private String typeInput;   // CHECKLIST atau NUMERIC

    // Menyimpan nilai per bulan. Key: 1..12 (bulan). Value: "Dilakukan" atau angka (string)
    private Map<Integer, String> values = new HashMap<>();

    // Menyimpan kondisi per bulan (GOOD, WARNING, DANGER) untuk mewarnai cell di Angular
    private Map<Integer, String> conditions = new HashMap<>();
}
