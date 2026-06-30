package com.lab.monitoring.repository;

import com.lab.monitoring.dto.ChartDataDTO;
import com.lab.monitoring.entity.MaintenanceRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface MaintenanceRecordRepository extends JpaRepository<MaintenanceRecord, Integer> {

    // (Dipakai pada tahap awal) Ambil record berdasarkan parameter dan tahun
    @Query("SELECT r FROM MaintenanceRecord r WHERE r.parameter.id = :parameterId AND EXTRACT(YEAR FROM r.recordMonth) = :year")
    List<MaintenanceRecord> findByParameterIdAndYear(@Param("parameterId") Integer parameterId, @Param("year") Integer year);

    // Optimasi (hindari N+1): Ambil semua record untuk instrument + tahun dalam sekali query
    @Query("SELECT r FROM MaintenanceRecord r WHERE r.parameter.instrument.id = :instrumentId AND EXTRACT(YEAR FROM r.recordMonth) = :year")
    List<MaintenanceRecord> findByInstrumentIdAndYear(@Param("instrumentId") Integer instrumentId, @Param("year") Integer year);

    // True upsert: cari record unik berdasarkan (parameter, recordMonth)
    Optional<MaintenanceRecord> findByParameterIdAndRecordMonth(Integer parameterId, LocalDate recordMonth);

    // 1. Checklist frequency per month (count of true checklistStatus)
    // Return Object[]: [month(Number), count(Number)]
    @Query("SELECT EXTRACT(MONTH FROM r.recordMonth), COUNT(r) " +
            "FROM MaintenanceRecord r " +
            "WHERE r.parameter.id = :parameterId " +
            "AND EXTRACT(YEAR FROM r.recordMonth) = :year " +
            "AND r.checklistStatus = true " +
            "GROUP BY EXTRACT(MONTH FROM r.recordMonth) " +
            "ORDER BY EXTRACT(MONTH FROM r.recordMonth)")
    List<Object[]> getChecklistFrequencySummary(@Param("parameterId") Integer parameterId, @Param("year") Integer year);

    // 2. Numeric trend per month (avg numeric value)
    // Return Object[]: [month(Number), avg(Number)]
    @Query("SELECT new com.lab.monitoring.dto.ChartDataDTO(" +
            "EXTRACT(MONTH FROM r.recordMonth), " +
            "null, " +
            "r.numericValue, " +
            "CAST(r.itemCondition AS string), " +
            "r.notes) " +
            "FROM MaintenanceRecord r " +
            "WHERE r.parameter.id = :parameterId " +
            "AND EXTRACT(YEAR FROM r.recordMonth) = :year " +
            "ORDER BY r.recordMonth ASC")
    List<ChartDataDTO> getNumericTrendLog(@Param("parameterId") Integer parameterId, @Param("year") int year);

    List<MaintenanceRecord> findAllByRecordMonth(LocalDate recordMonth);
}
