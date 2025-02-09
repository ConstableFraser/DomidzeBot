package org.shvedchikov.domidzebot.component;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import static java.lang.System.getProperty;
import static java.lang.System.setProperty;

import static org.shvedchikov.domidzebot.util.CoderDecoder.encodeString;

@Component
public class DataInitializer implements ApplicationRunner {

    @Override
    public void run(ApplicationArguments args) {
        var prop = "pwdprop";
        var value = "raz dva tri pop is nutri tri dva raz vot i raskolbas. Only one man warrior in field";
        //TODO set should be at admin bot

        if (getProperty(prop, "").isEmpty()) {
            var codeProp = encodeString(value);
            setProperty(prop, codeProp);
        }
    }
}
