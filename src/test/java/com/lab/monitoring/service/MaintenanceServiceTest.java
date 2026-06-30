package com.lab.monitoring.service;

import com.lab.monitoring.dto.PagedResponseDTO;
import com.lab.monitoring.entity.Enums;
import com.lab.monitoring.entity.MaintenanceParameter;
import com.lab.monitoring.entity.MaintenanceRecord;
import com.lab.monitoring.repository.MaintenanceParameterRepository;
import com.lab.monitoring.repository.MaintenanceRecordRepository;
import com.lab.monitoring.repository.UserRepository;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class MaintenanceServiceTest {

    @Test
    void getGridData_shouldPaginateAndPivotWithoutNPlusOne() {
        MaintenanceParameterRepository paramRepo = mock(MaintenanceParameterRepository.class);
        MaintenanceRecordRepository recordRepo = mock(MaintenanceRecordRepository.class);
        UserRepository userRepo = mock(UserRepository.class);

        MaintenanceParameter p1 = new MaintenanceParameter();
        p1.setId(1);
        p1.setParameterName("P1");
        p1.setTypeInput(Enums.InputType.NUMERIC);

        MaintenanceParameter p2 = new MaintenanceParameter();
        p2.setId(2);
        p2.setParameterName("P2");
        p2.setTypeInput(Enums.InputType.CHECKLIST);

        when(paramRepo.findByInstrumentId(10)).thenReturn(List.of(p1, p2));

        MaintenanceRecord rJan = new MaintenanceRecord();
        rJan.setId(100);
        rJan.setParameter(p1);
        rJan.setRecordMonth(LocalDate.of(2026, 1, 1));
        rJan.setNumericValue(new BigDecimal("123"));
        rJan.setItemCondition(Enums.ComponentCondition.GOOD);

        when(recordRepo.findByInstrumentIdAndYear(10, 2026)).thenReturn(List.of(rJan));

        MaintenanceService svc = new MaintenanceService(paramRepo, recordRepo, userRepo);

        PagedResponseDTO<?> resp = svc.getGridData(10, 2026, 0, 1, null, null);
        assertEquals(2, resp.getMeta().getLength());
        assertEquals(1, resp.getData().size());

        // pastikan query bulk dipanggil sekali
        verify(recordRepo, times(1)).findByInstrumentIdAndYear(10, 2026);
    }

    @Test
    void saveRecord_shouldUpsertByParameterAndMonth() {
        MaintenanceParameterRepository paramRepo = mock(MaintenanceParameterRepository.class);
        MaintenanceRecordRepository recordRepo = mock(MaintenanceRecordRepository.class);
        UserRepository userRepo = mock(UserRepository.class);

        MaintenanceParameter p = new MaintenanceParameter();
        p.setId(1);
        p.setTypeInput(Enums.InputType.NUMERIC);
        p.setThresholdOperator(">=");
        p.setThresholdValue(new BigDecimal("10"));

        when(paramRepo.findById(1)).thenReturn(Optional.of(p));

        MaintenanceRecord existing = new MaintenanceRecord();
        existing.setId(99);
        existing.setParameter(p);
        existing.setRecordMonth(LocalDate.of(2026, 2, 1));

        when(recordRepo.findByParameterIdAndRecordMonth(1, LocalDate.of(2026, 2, 1))).thenReturn(Optional.of(existing));
        when(recordRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        MaintenanceService svc = new MaintenanceService(paramRepo, recordRepo, userRepo);

        var dto = new com.lab.monitoring.dto.RecordSubmitDTO();
        dto.setParameterId(1);
        dto.setYear(2026);
        dto.setMonth(2);
        dto.setValue("25");

        MaintenanceRecord saved = svc.saveRecord(dto);
        assertEquals(99, saved.getId());
        assertEquals(new BigDecimal("25"), saved.getNumericValue());
        assertEquals(Enums.ComponentCondition.DANGER, saved.getItemCondition());

        verify(recordRepo, times(1)).findByParameterIdAndRecordMonth(1, LocalDate.of(2026, 2, 1));
    }
}
