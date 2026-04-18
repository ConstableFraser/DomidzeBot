package org.shvedchikov.domidzebot.controller;

import org.shvedchikov.domidzebot.config.BotConfig;
import org.shvedchikov.domidzebot.service.CommonCalendarService;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.ui.Model;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

@Controller
@RequestMapping("/")
public class HomePageController {
    private static final DateTimeFormatter DTF = DateTimeFormatter.ofPattern("dd.MM.yyyy");
    private final CommonCalendarService commonCalendarService;
    private final BotConfig botConfig;

    public HomePageController(CommonCalendarService commonCalendarService,
                              BotConfig botConfig) {
        this.commonCalendarService = commonCalendarService;
        this.botConfig = botConfig;
    }

    @GetMapping("")
    public String index(Model model) {
        int countWeeksCalendar = botConfig.getWeeks();
        Map<String, String> month = new HashMap<>() {
            {
                put("01", "Я Н В А Р Ь");
                put("02", "Ф Е В Р А Л Ь");
                put("03", "М А Р Т");
                put("04", "А П Р Е Л Ь");
                put("05", "М А Й");
                put("06", "И Ю Н Ь");
                put("07", "И Ю Л Ь");
                put("08", "А В Г У С Т");
                put("09", "С Е Н Т Я Б Р Ь");
                put("10", "О К Т Я Б Р Ь");
                put("11", "Н О Я Б Р Ь");
                put("12", "Д Е К А Б Р Ь");
            }
        };
        model.addAttribute("calendar", commonCalendarService.getTableOfCalendar());
        model.addAttribute("nextMonthDate", LocalDate.now().plusMonths(1).format(DTF));
        model.addAttribute("month", month);
        model.addAttribute("countWeeks", countWeeksCalendar);
        return "index";
    }
}
