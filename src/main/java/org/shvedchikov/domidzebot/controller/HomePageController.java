package org.shvedchikov.domidzebot.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.GetMapping;

import java.net.URI;

@RestController
public class HomePageController {
    @GetMapping(path = "/welcome")
    public String welcome() {
        return ":: Welcome to DomidzeBot ::";
    }

    @GetMapping("/")
    public ResponseEntity<Void> home() {
        return ResponseEntity.status(HttpStatus.FOUND)
                .location(URI.create("/welcome"))
                .build();
    }
}
