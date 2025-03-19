package org.shvedchikov.domidzebot.config;

import lombok.Data;
// import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
// import org.springframework.context.annotation.PropertySource;

@Data
@Configuration
// @PropertySource("classpath:application.properties")
public class AppProperties {
//    @Value(value = "${appproperties.hash}")
    private final String hash = """
        djM7MT82MzYxgkA9iYtLdzeFSESCpzCuOptoS0mLMjKRT1dLRTE\
        wUDBINk5rSzVFQDVFP4kzNjE9OTM2MTQzlUg2M001RT42SFE=""";
}
