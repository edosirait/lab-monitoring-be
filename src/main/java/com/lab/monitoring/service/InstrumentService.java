package com.lab.monitoring.service;

import com.lab.monitoring.dto.PageMetaDTO;
import com.lab.monitoring.dto.PagedResponseDTO;
import com.lab.monitoring.entity.Instrument;
import com.lab.monitoring.repository.InstrumentRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class InstrumentService {

    private final InstrumentRepository instrumentRepository;

    public InstrumentService(InstrumentRepository instrumentRepository) {
        this.instrumentRepository = instrumentRepository;
    }

    public List<Instrument> findAll() {
        return instrumentRepository.findAll();
    }

    public PagedResponseDTO<Instrument> findAllPaged(Integer page, Integer size, Integer offset, Integer limit) {
        List<Instrument> all = instrumentRepository.findAll();

        int resolvedSize = (size != null && size > 0) ? size : 20;
        int resolvedPage = (page != null && page >= 0) ? page : 0;
        int resolvedOffset;
        int resolvedLimit;

        if (offset != null || limit != null) {
            resolvedOffset = Math.max(0, offset != null ? offset : 0);
            resolvedLimit = Math.max(1, limit != null ? limit : resolvedSize);
            resolvedPage = resolvedOffset / resolvedLimit;
            resolvedSize = resolvedLimit;
        } else {
            resolvedOffset = resolvedPage * resolvedSize;
            resolvedLimit = resolvedSize;
        }

        long length = all.size();
        int from = Math.min(resolvedOffset, all.size());
        int to = Math.min(resolvedOffset + resolvedLimit, all.size());

        PageMetaDTO meta = new PageMetaDTO(length, resolvedPage, resolvedSize, resolvedOffset, resolvedLimit);
        return new PagedResponseDTO<>(meta, all.subList(from, to));
    }
}
