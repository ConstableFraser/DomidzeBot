package org.shvedchikov.domidzebot.config;

import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

@Data
@Configuration
//@PropertySource(value = "file:///home/runner/work/DomidzeBot/DomidzeBot/src/main/resources/application.properties")
@PropertySource(value = "classpath:${PATHPROPERTY:}application.properties")
public class BotConfig {
    @Value(value = "${bot.name}")
    private String name;

    @Value(value = "${bot.token}")
    private String token;
}
