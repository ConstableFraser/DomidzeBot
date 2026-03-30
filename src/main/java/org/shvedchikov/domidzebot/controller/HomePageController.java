package org.shvedchikov.domidzebot.controller;

import org.shvedchikov.domidzebot.service.CommonCalendarService;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.ui.Model;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@Controller
@RequestMapping("/")
public class HomePageController {
    private static final DateTimeFormatter DTF = DateTimeFormatter.ofPattern("dd.MM.yyyy");
    private final CommonCalendarService commonCalendarService;

    public HomePageController(CommonCalendarService commonCalendarService) {
        this.commonCalendarService = commonCalendarService;
    }

    @GetMapping("")
    public String index(Model model) {
        model.addAttribute("calendar", commonCalendarService.getTableOfCalendar());
        model.addAttribute("nextMonthDate", LocalDate.now().plusMonths(1).format(DTF));
        return "index";
    }
}
