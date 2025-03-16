package org.shvedchikov.domidzebot;

import lombok.extern.slf4j.Slf4j;
import net.datafaker.Faker;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.web.bind.annotation.RestController;

@EnableTransactionManagement
@SpringBootApplication
@EnableJpaAuditing
@RestController
@Slf4j
public class DomidzeBotApplication {

    public static void main(String[] args) {
        SpringApplication.run(DomidzeBotApplication.class, args);
    }

    @Bean
    public Faker getFaker() {
        return new Faker();
    }
}
