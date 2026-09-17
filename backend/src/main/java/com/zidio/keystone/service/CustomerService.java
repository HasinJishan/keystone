package com.zidio.keystone.service;

import com.zidio.keystone.domain.Customer;
import com.zidio.keystone.dto.CustomerDto;
import com.zidio.keystone.exception.ResourceNotFoundException;
import com.zidio.keystone.repository.CustomerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CustomerService {

    private final CustomerRepository customerRepository;

    @Transactional
    public CustomerDto create(CustomerDto dto) {
        Customer c = Customer.builder()
                .name(dto.name())
                .contactEmail(dto.contactEmail())
                .contactPhone(dto.contactPhone())
                .build();
        c = customerRepository.save(c);
        return toDto(c);
    }

    @Transactional
    public CustomerDto update(Long id, CustomerDto dto) {
        Customer c = customerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found: " + id));
        c.setName(dto.name());
        c.setContactEmail(dto.contactEmail());
        c.setContactPhone(dto.contactPhone());
        return toDto(c);
    }

    public CustomerDto get(Long id) {
        return customerRepository.findById(id)
                .map(this::toDto)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found: " + id));
    }

    public Page<CustomerDto> search(String q, Pageable pageable) {
        Page<Customer> page = (q == null || q.isBlank())
                ? customerRepository.findAll(pageable)
                : customerRepository.findByNameContainingIgnoreCase(q, pageable);
        return page.map(this::toDto);
    }

    @Transactional
    public void delete(Long id) {
        if (!customerRepository.existsById(id)) {
            throw new ResourceNotFoundException("Customer not found: " + id);
        }
        customerRepository.deleteById(id);
    }

    private CustomerDto toDto(Customer c) {
        return new CustomerDto(c.getId(), c.getName(), c.getContactEmail(), c.getContactPhone());
    }
}
