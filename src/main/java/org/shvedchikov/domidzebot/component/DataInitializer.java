package org.shvedchikov.domidzebot.component;

import org.shvedchikov.domidzebot.dto.domain.DomainCreateDTO;
import org.shvedchikov.domidzebot.service.DomainService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
public class DataInitializer implements ApplicationRunner {
    @Autowired
    private DomainService domainService;

    @Override
    public void run(ApplicationArguments args) {
        if (!domainService.getAll().isEmpty()) {
            return;
        }
        var domainCreateDTO = new DomainCreateDTO();
        domainCreateDTO.setDomain("ethnomir.ru");
        domainService.create(domainCreateDTO);
    }
}
