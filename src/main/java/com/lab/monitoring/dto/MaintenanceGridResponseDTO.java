package com.lab.monitoring.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * @deprecated Digantikan oleh {@link PagedResponseDTO} untuk response grid + pagination.
 */
@Deprecated
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MaintenanceGridResponseDTO {
    private Integer instrumentId;
    private Integer year;
    private List<DataGridRowDTO> rows;
}
