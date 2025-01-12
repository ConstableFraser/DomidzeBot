package org.shvedchikov.domidzebot.controller;

import jakarta.validation.Valid;
import org.shvedchikov.domidzebot.dto.domain.DomainCreateDTO;
import org.shvedchikov.domidzebot.dto.domain.DomainDTO;
import org.shvedchikov.domidzebot.dto.domain.DomainUpdateDTO;
import org.shvedchikov.domidzebot.service.DomainService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api")
public class DomainsController {
    @Autowired
    private DomainService domainService;

    @GetMapping("/domains")
    @ResponseStatus(HttpStatus.OK)
    public ResponseEntity<List<DomainDTO>> index() {
        var domains = domainService.getAll();

        return ResponseEntity.ok()
                .header("X-Total-Count", String.valueOf(domains.size()))
                .body(domains);
    }

    @PostMapping("/domains")
    @ResponseStatus(HttpStatus.CREATED)
    public DomainDTO create(@Valid @RequestBody DomainCreateDTO domainCreateDTO) {
        return domainService.create(domainCreateDTO);
    }

    @GetMapping("/domains/{id}")
    @ResponseStatus(HttpStatus.OK)
    public DomainDTO show(@PathVariable Long id) {
        return domainService.show(id);
    }

    @PutMapping("/domains/{id}")
    @ResponseStatus(HttpStatus.OK)
    public DomainDTO update(@RequestBody DomainUpdateDTO domainData, @PathVariable Long id) {
        return domainService.update(domainData, id);
    }

    @DeleteMapping("/domains/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void destroy(@PathVariable Long id) {
        domainService.destroy(id);
    }
}
