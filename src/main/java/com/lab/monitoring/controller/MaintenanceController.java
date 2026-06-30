package com.lab.monitoring.controller;

import com.lab.monitoring.dto.ChartDataDTO;
import com.lab.monitoring.dto.DataGridRowDTO;
import com.lab.monitoring.dto.PagedResponseDTO;
import com.lab.monitoring.dto.RecordSubmitDTO;
import com.lab.monitoring.entity.MaintenanceActivityLog;
import com.lab.monitoring.entity.MaintenanceRecord;
import com.lab.monitoring.repository.MaintenanceActivityLogRepository;
import com.lab.monitoring.service.MaintenanceService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/maintenances")
public class MaintenanceController {

    private final MaintenanceService maintenanceService;
    private final MaintenanceActivityLogRepository activityLogRepository;

    public MaintenanceController(MaintenanceService maintenanceService,
                                 MaintenanceActivityLogRepository activityLogRepository) {
        this.maintenanceService = maintenanceService;
        this.activityLogRepository = activityLogRepository;
    }

    @GetMapping("/grid/{instrumentId}")
    public PagedResponseDTO<DataGridRowDTO> getGrid(@PathVariable Integer instrumentId,
                                                    @RequestParam int year,
                                                    @RequestParam(required = false) Integer page,
                                                    @RequestParam(required = false) Integer size,
                                                    @RequestParam(required = false) Integer offset,
                                                    @RequestParam(required = false) Integer limit,
                                                    @RequestParam(name = "start", required = false) Integer start,
                                                    @RequestParam(name = "length", required = false) Integer length) {
        // Normalize common param names: if frontend sends start/length (DataTables), map them to offset/size
        Integer resolvedOffset = offset;
        Integer resolvedLimit = limit;
        Integer resolvedSize = size;
        Integer resolvedPage = page;

        if (start != null) {
            resolvedOffset = start;
        }
        if (length != null) {
            resolvedSize = length;
            resolvedLimit = length;
        }

        return maintenanceService.getGridData(instrumentId, year, resolvedPage, resolvedSize, resolvedOffset, resolvedLimit);
    }

    @PostMapping("/record")
    public MaintenanceRecord saveRecord(@RequestBody RecordSubmitDTO dto) {
        return maintenanceService.saveRecord(dto);
    }

    // New chart endpoints
    @GetMapping("/chart/checklist/{parameterId}")
    public List<ChartDataDTO> getChecklistChart(@PathVariable Integer parameterId, @RequestParam int year) {
        return maintenanceService.getChecklistChart(parameterId, year);
    }

    @GetMapping("/chart/numeric/{parameterId}")
    public List<ChartDataDTO> getNumericChart(@PathVariable Integer parameterId, @RequestParam int year) {
        return maintenanceService.getNumericChart(parameterId, year);
    }

    @GetMapping("/parameters/{parameterId}/charts/numeric-history")
    public ResponseEntity<List<ChartDataDTO>> getNumericHistoryChart(
            @PathVariable Integer parameterId,
            @RequestParam int year) {

        List<ChartDataDTO> dbRecords = activityLogRepository.findHistoryByParameterAndYear(parameterId, year);

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

        return ResponseEntity.ok(out);
    }
}
