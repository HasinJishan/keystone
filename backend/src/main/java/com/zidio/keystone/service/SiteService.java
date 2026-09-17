package com.zidio.keystone.service;

import com.zidio.keystone.domain.Site;
import com.zidio.keystone.dto.SiteDto;
import com.zidio.keystone.exception.ResourceNotFoundException;
import com.zidio.keystone.repository.CustomerRepository;
import com.zidio.keystone.repository.SiteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SiteService {

    private final SiteRepository siteRepository;
    private final CustomerRepository customerRepository;

    @Transactional
    public SiteDto create(SiteDto dto) {
        if (!customerRepository.existsById(dto.customerId())) {
            throw new ResourceNotFoundException("Customer not found: " + dto.customerId());
        }
        Site site = Site.builder()
                .customerId(dto.customerId())
                .name(dto.name())
                .address(dto.address())
                .build();
        site = siteRepository.save(site);
        return toDto(site);
    }

    public List<SiteDto> byCustomer(Long customerId) {
        return siteRepository.findByCustomerId(customerId).stream().map(this::toDto).toList();
    }

    @Transactional
    public SiteDto update(Long id, SiteDto dto) {
        Site site = siteRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Site not found: " + id));
        site.setName(dto.name());
        site.setAddress(dto.address());
        return toDto(site);
    }

    private SiteDto toDto(Site s) {
        return new SiteDto(s.getId(), s.getCustomerId(), s.getName(), s.getAddress());
    }
}
