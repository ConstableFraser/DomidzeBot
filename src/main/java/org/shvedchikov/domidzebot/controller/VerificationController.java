package org.shvedchikov.domidzebot.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
public class VerificationController {

    @GetMapping("/yandex_a018a11cf524c193.html")
    @ResponseBody
    public String yandexVerification() {
        return "Verification: a018a11cf524c193";
    }
}
