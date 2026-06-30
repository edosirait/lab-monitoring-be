package com.lab.monitoring.repository;

import com.lab.monitoring.entity.Notification;
import com.lab.monitoring.entity.IncidentReport;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface IncidentReportRepository extends JpaRepository<IncidentReport, Integer> {

    List<IncidentReport> findByInstrumentIdOrderByReportDateDesc(Integer instrumentId);

    List<IncidentReport> findAllByOrderByReportDateDesc();

    List<IncidentReport> findByInstrumentIsNullOrderByReportDateDesc();

    long countByStatus(String status);
}
