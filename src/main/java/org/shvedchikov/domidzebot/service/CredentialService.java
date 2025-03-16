package org.shvedchikov.domidzebot.service;

import jakarta.validation.Valid;
import org.shvedchikov.domidzebot.dto.credential.CredentialCreateDTO;
import org.shvedchikov.domidzebot.dto.credential.CredentialDTO;
import org.shvedchikov.domidzebot.dto.credential.CredentialUpdateDTO;
import org.shvedchikov.domidzebot.exception.ResourceNotFoundException;
import org.shvedchikov.domidzebot.mapper.CredentialMapper;
import org.shvedchikov.domidzebot.repository.CredentialRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;

@Service
public class CredentialService {
    @Autowired
    private CredentialRepository credentialRepository;

    @Autowired
    private CredentialMapper credentialMapper;

    public List<CredentialDTO> getAll() {
        var credentials = credentialRepository.findAll();
        return credentials.stream()
                .map(credentialMapper::map)
                .toList();
    }

    public CredentialDTO create(@Valid @RequestBody CredentialCreateDTO credentialCreateDTO) {
        var credential = credentialMapper.map(credentialCreateDTO);
        credentialRepository.save(credential);

        return credentialMapper.map(credential);
    }

    @Transactional
    public CredentialDTO update(@RequestBody CredentialUpdateDTO credentialData, @PathVariable Long id) {
        var credential = credentialRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Credential with id: " + id + " not found"));
        credentialMapper.update(credentialData, credential);
        credential = credentialRepository.save(credential);

        return credentialMapper.map(credential);
    }

    public CredentialDTO show(Long id) {
        var credential = credentialRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Credential with id: " + id + " not found"));
        return credentialMapper.map(credential);
    }

    @Transactional
    public void destroy(@PathVariable Long id) {
        credentialRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Credential with id: " + id + " not found"));
        credentialRepository.deleteById(id);
    }

    public void autoSignIn(Long id) {
        var credential = credentialRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Credential with id: " + id + " not found"));
        System.out.println(credential.getPassword());
        //TODO for auto sign in;
    }
}
