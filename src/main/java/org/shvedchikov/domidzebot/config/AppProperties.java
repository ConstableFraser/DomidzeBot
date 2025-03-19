package org.shvedchikov.domidzebot.config;

import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

@Data
@Configuration
@PropertySource(value = "classpath:application.properties")
public class AppProperties {
    @Value(value = "${appproperties.hash:4kzNjE9OTM2MTQzlUg2M001RT42SFE=}")
    private String hash;
}
