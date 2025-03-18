package org.shvedchikov.domidzebot.config;

import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
public class BotConfig {
    @Value(value = "${bot.name}")
    private String name;

    @Value(value = "${bot.token}")
    private String token;
}
