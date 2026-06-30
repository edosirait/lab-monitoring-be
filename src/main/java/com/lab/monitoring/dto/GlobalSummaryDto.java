package com.lab.monitoring.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class GlobalSummaryDto {
    private long totalInstruments;
    private long criticalInstruments;
    private double complianceRate;
    private long openIncidents;
    private Map<String, Long> conditionDistribution;
    private List<Map<String, Object>> monthlyTrend;
    private List<Map<String, Object>> criticalInstrumentsDetail;
}
