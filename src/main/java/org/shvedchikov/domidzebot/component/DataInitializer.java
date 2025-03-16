package org.shvedchikov.domidzebot.component;

import org.shvedchikov.domidzebot.dto.domain.DomainCreateDTO;
import org.shvedchikov.domidzebot.service.DomainService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import static java.lang.System.getProperty;
import static java.lang.System.setProperty;

import static org.shvedchikov.domidzebot.util.CoderDecoder.encodeString;

@Component
public class DataInitializer implements ApplicationRunner {
    @Autowired
    private DomainService domainService;

    @Override
    public void run(ApplicationArguments args) {
        var prop = "pwdprop";
        var value = "raz dva tri pop is nutri tri dva raz vot i raskolbas. Only one man warrior in field";
        //TODO set should be at admin bot

        if (getProperty(prop, "").isEmpty()) {
            var codeProp = encodeString(value);
            setProperty(prop, codeProp);
        }
        var domainCreateDTO = new DomainCreateDTO();
        domainCreateDTO.setDomain("ethnomir.ru");
        domainService.create(domainCreateDTO);
        domainCreateDTO.setDomain("bnovo.ru");
        domainService.create(domainCreateDTO);
    }
}
