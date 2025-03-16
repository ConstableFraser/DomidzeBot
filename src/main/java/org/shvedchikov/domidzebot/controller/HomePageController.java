package org.shvedchikov.domidzebot.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.PropertySource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.GetMapping;

import java.net.URI;

@RestController
@PropertySource(value = "classpath:application.properties")
public class HomePageController {
    @Value("${spring.application.name}")
    private String name;

    @Value("${spring.application.version}")
    private String version;

    @GetMapping(path = "/welcome")
    public String welcome() {
        return "\uD83C\uDFE0 Welcome to "
                + name + "! \uD83C\uDFE0"
                + "<div><a href=\"https://www.t.me/" + name + "\">" + "@" + name + "</a></div>"
                + "<br><br>version: " + version;
    }

    @GetMapping("/")
    public ResponseEntity<Void> home() {
        return ResponseEntity.status(HttpStatus.FOUND)
                .location(URI.create("/welcome"))
                .build();
    }
}
