package com.lab.monitoring.repository;

import com.lab.monitoring.dto.ChartDataDTO;
import com.lab.monitoring.entity.MaintenanceActivityLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MaintenanceActivityLogRepository extends JpaRepository<MaintenanceActivityLog, Long> {

    @Query("SELECT new com.lab.monitoring.dto.ChartDataDTO(" +
            "EXTRACT(MONTH FROM l.recordMonth), " + // 🔥 UBAH DI SINI: dari logDate ke recordMonth
            "null, " +
            "l.previousValue, " +
            "'DANGER', " +
            "l.notes) " +
            "FROM MaintenanceActivityLog l " +
            "WHERE l.parameter.id = :parameterId " +
            "AND EXTRACT(YEAR FROM l.recordMonth) = :year " + // 🔥 UBAH DI SINI JUGA
            "ORDER BY l.recordMonth ASC")
    List<ChartDataDTO> findHistoryByParameterAndYear(
            @Param("parameterId") Integer parameterId,
            @Param("year") int year
    );
}
