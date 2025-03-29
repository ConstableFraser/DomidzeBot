package org.shvedchikov.domidzebot.config;

import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

import java.util.Objects;

@Data
@Configuration
@PropertySource(value = "classpath:${PATHPROPERTY:}application.properties")
public class BotConfig {
    @Value(value = "${bot.name}")
    private String name;

    @Value(value = "${bot.token}")
    private String token;

    @Value(value = "${bot.admin}")
    private Long idAdmin;

    public boolean isNoAdmin(Long userId) {
        if (Objects.isNull(userId) || Objects.isNull(idAdmin)) {
            return true;
        }
        return !Objects.equals(idAdmin, userId);
    }
}
