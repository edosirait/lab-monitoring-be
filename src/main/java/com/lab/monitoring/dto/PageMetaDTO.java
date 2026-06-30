package com.lab.monitoring.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PageMetaDTO {
    // total data (sebelum pagination)
    private long length;

    // standard page-based pagination
    private int page;
    private int size;

    // offset/limit style
    private int offset;
    private int limit;
}
