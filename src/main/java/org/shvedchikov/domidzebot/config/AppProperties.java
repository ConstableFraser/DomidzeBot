package org.shvedchikov.domidzebot.config;

import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

@Data
@Configuration
@PropertySource(value = "file:///home/runner/work/DomidzeBot/DomidzeBot/src/test/resources/application.properties")
public class AppProperties {
    @Value(value = "${appproperties.hash:default}")
    private String hash;
}
