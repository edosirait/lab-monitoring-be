package com.lab.monitoring.controller;

import com.lab.monitoring.dto.PagedResponseDTO;
import com.lab.monitoring.entity.Instrument;
import com.lab.monitoring.service.InstrumentService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/instruments")
public class InstrumentController {

    private final InstrumentService instrumentService;

    public InstrumentController(InstrumentService instrumentService) {
        this.instrumentService = instrumentService;
    }

    @GetMapping
    public PagedResponseDTO<Instrument> getAll(@RequestParam(required = false) Integer page,
                                               @RequestParam(required = false) Integer size,
                                               @RequestParam(required = false) Integer offset,
                                               @RequestParam(required = false) Integer limit) {
        return instrumentService.findAllPaged(page, size, offset, limit);
    }
}
