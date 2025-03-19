package org.shvedchikov.domidzebot.config;

import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

@Data
@Configuration
@PropertySource(value = "classpath:application.properties")
public class BotConfig {
    @Value(value = "${bot.name:domidzebot}")
    private String name;

    @Value(value = "${bot.token:2356546:WGWGWGHWHWRHWHWH}")
    private String token;
}
