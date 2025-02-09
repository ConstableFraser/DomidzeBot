package org.shvedchikov.domidzebot.controller;

import jakarta.validation.Valid;
import org.shvedchikov.domidzebot.dto.credential.CredentialCreateDTO;
import org.shvedchikov.domidzebot.dto.credential.CredentialDTO;
import org.shvedchikov.domidzebot.dto.credential.CredentialUpdateDTO;
import org.shvedchikov.domidzebot.service.CredentialService;
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
public class CredentialsController {

    @Autowired
    private CredentialService credentialService;

    @GetMapping("/credentials")
    @ResponseStatus(HttpStatus.OK)
    public ResponseEntity<List<CredentialDTO>> index() {
        var credentials = credentialService.getAll();

        return ResponseEntity.ok()
                .header("X-Total-Count", String.valueOf(credentials.size()))
                .body(credentials);
    }

    @PostMapping("/credentials")
    @ResponseStatus(HttpStatus.CREATED)
    public CredentialDTO create(@Valid @RequestBody CredentialCreateDTO credentialCreateDTO) {
        return credentialService.create(credentialCreateDTO);
    }

    @GetMapping("/credentials/{id}")
    @ResponseStatus(HttpStatus.OK)
    public CredentialDTO show(@PathVariable Long id) {
        return credentialService.show(id);
    }

    @PutMapping("/credentials/{id}")
    @ResponseStatus(HttpStatus.OK)
    public CredentialDTO update(@Valid @RequestBody CredentialUpdateDTO credentialData, @PathVariable Long id) {
        return credentialService.update(credentialData, id);
    }

    @DeleteMapping("/credentials/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void destroy(@PathVariable Long id) {
        credentialService.destroy(id);
    }

    @PostMapping("/credentials/autoSignIn/{id}")
    @ResponseStatus(HttpStatus.OK)
    public void autoSignIn(@PathVariable Long id) {
        try {
            credentialService.autoSignIn(id);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
