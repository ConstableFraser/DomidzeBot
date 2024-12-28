package org.shvedchikov.domidzebot;

import net.datafaker.Faker;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.web.bind.annotation.RestController;

@SpringBootApplication
@RestController
@EnableJpaAuditing
public class DomidzeBotApplication {

    public static void main(String[] args) {
        SpringApplication.run(DomidzeBotApplication.class, args);
    }

    @Bean
    public Faker getFaker() {
        return new Faker();
    }
}
