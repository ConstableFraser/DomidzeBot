package org.shvedchikov.domidzebot;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

@SpringBootApplication
@RestController
public class DomidzeBotApplication {

    public static void main(String[] args) {
        SpringApplication.run(DomidzeBotApplication.class, args);
    }

    @GetMapping("/welcome")
    public String welcome() {
        return ":: home page ::";
    }

    @GetMapping("/")
    public ResponseEntity<Void> home() {
        return ResponseEntity.status(HttpStatus.FOUND)
                .location(URI.create("/welcome"))
                .build();
    }
}
