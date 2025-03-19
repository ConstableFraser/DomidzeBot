package org.shvedchikov.domidzebot.config;

import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

@Data
@Configuration
@PropertySource("file:///src/main/test/resources/application-test.properties")
public class AppProperties {
    @Value(value = "${appproperties.hash}")
    private String hash;
}
