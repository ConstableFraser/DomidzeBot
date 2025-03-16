package org.shvedchikov.domidzebot.service;

import jakarta.validation.Valid;
import org.shvedchikov.domidzebot.dto.domain.DomainCreateDTO;
import org.shvedchikov.domidzebot.dto.domain.DomainDTO;
import org.shvedchikov.domidzebot.dto.domain.DomainUpdateDTO;
import org.shvedchikov.domidzebot.exception.ResourceNotFoundException;
import org.shvedchikov.domidzebot.mapper.DomainMapper;
import org.shvedchikov.domidzebot.repository.DomainRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;

@Service
public class DomainService {
    @Autowired
    private DomainRepository domainRepository;

    @Autowired
    private DomainMapper domainMapper;

    public List<DomainDTO> getAll() {
        var domains = domainRepository.findAll();
        return domains.stream()
                .map(domainMapper::map)
                .toList();
    }

    @Transactional
    public DomainDTO create(@Valid @RequestBody DomainCreateDTO domainCreateDTO) {
        var domain = domainMapper.map(domainCreateDTO);
        domainRepository.save(domain);

        return domainMapper.map(domain);
    }

    @Transactional
    public DomainDTO update(@RequestBody DomainUpdateDTO domainUpdateDTO, @PathVariable Long id) {
        var domain = domainRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Domain with id: " + id + " not found"));
        domainMapper.update(domainUpdateDTO, domain);
        domain = domainRepository.save(domain);

        return domainMapper.map(domain);
    }

    public DomainDTO show(Long id) {
        var domain = domainRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Domain with id: " + id + " not found"));
        return domainMapper.map(domain);
    }

    @Transactional
    public void destroy(@PathVariable Long id) {
        domainRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Domain with id: " + id + " not found"));
        domainRepository.deleteById(id);
    }
}
