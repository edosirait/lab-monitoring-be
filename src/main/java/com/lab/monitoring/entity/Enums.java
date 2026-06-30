package com.lab.monitoring.entity;

/**
 * Kumpulan enum agar isi kolom di database konsisten (tidak typo).
 */
public class Enums {

    public enum UserRole {
        STAFF,
        SUPERVISOR,
        ADMIN
    }

    /**
     * Jenis input untuk parameter maintenance.
     * CHECKLIST: Ya/Tidak
     * NUMERIC: Angka (mis. jam, suhu, tekanan)
     */
    public enum InputType {
        CHECKLIST,
        NUMERIC
    }

    /**
     * Kondisi komponen setelah dilakukan pengecekan.
     */
    public enum ComponentCondition {
        GOOD,
        WARNING,
        DANGER
    }

    public enum IncidentStatus {
        OPEN,
        IN_PROGRESS,
        RESOLVED
    }
}
