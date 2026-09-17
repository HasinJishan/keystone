package com.zidio.keystone.service;

import com.zidio.keystone.domain.Part;
import com.zidio.keystone.dto.PartDto;
import com.zidio.keystone.exception.ResourceNotFoundException;
import com.zidio.keystone.repository.PartRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PartService {

    private final PartRepository partRepository;

    @Transactional
    public PartDto create(PartDto dto) {
        Part part = Part.builder()
                .name(dto.name())
                .sku(dto.sku())
                .unitCost(dto.unitCost())
                .stockQty(dto.stockQty())
                .build();
        part = partRepository.save(part);
        return toDto(part);
    }

    public List<PartDto> all() {
        return partRepository.findAll().stream().map(this::toDto).toList();
    }

    @Transactional
    public PartDto update(Long id, PartDto dto) {
        Part part = partRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Part not found: " + id));
        part.setName(dto.name());
        part.setSku(dto.sku());
        part.setUnitCost(dto.unitCost());
        part.setStockQty(dto.stockQty());
        return toDto(part);
    }

    @Transactional
    public void delete(Long id) {
        if (!partRepository.existsById(id)) {
            throw new ResourceNotFoundException("Part not found: " + id);
        }
        partRepository.deleteById(id);
    }

    private PartDto toDto(Part p) {
        return new PartDto(p.getId(), p.getName(), p.getSku(), p.getUnitCost(), p.getStockQty());
    }
}
