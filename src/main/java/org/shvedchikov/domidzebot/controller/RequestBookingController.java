package org.shvedchikov.domidzebot.controller;

import lombok.extern.slf4j.Slf4j;
import org.shvedchikov.domidzebot.dto.booking.BookingCreateDTO;
import org.shvedchikov.domidzebot.repository.UserRepository;
import org.shvedchikov.domidzebot.service.CommonCalendarService;
import org.shvedchikov.domidzebot.service.TelegramBotService;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@Slf4j
@Controller
@RequestMapping("/api")
public class RequestBookingController {
    private final UserRepository userRepository;
    private final TelegramBotService telegramBotService;
    private final CommonCalendarService commonCalendarService;

    private static final DateTimeFormatter DTF = DateTimeFormatter.ofPattern("dd.MM.yyyy");

    public RequestBookingController(UserRepository userRepository,
                                    TelegramBotService telegramBotService,
                                    CommonCalendarService commonCalendarService) {
        this.userRepository = userRepository;
        this.telegramBotService = telegramBotService;
        this.commonCalendarService = commonCalendarService;
    }

    @PostMapping("/booking")
    public String booking(BookingCreateDTO booking, RedirectAttributes redirectAttributes) {
        var calendar = commonCalendarService.getCommonCalendar();
        var users = userRepository.findByIsEnableIsTrue();
        String stringMessage = """
                ⭐️ пришла бронь с сайта ⭐️

                ЗАЕЗД  |  ВЫЕЗД  | ГОСТЕЙ | ЦЕНА | ТЕЛЕФОН
                --------------------------------------------------------------
                """;

        var classMessage = "alert alert-success alert-dismissible fade show";
        var messageTextMain = "Заявка принята!";
        var messageText = "Мы свяжемся с Вами в течение 30 минут";

        var checkIn = booking.getCheckIn();
        var checkOut = booking.getCheckOut();
        var mobileNumber = booking.getMobileNumber();

        if (commonCalendarService.getCountOfBookings(checkIn, checkOut) > 0) {
            messageTextMain = "Ошибка!";
            messageText = "Выбранные даты уже заняты, укажите пожалуйста другие";
            redirectAttributes.addFlashAttribute(
                    "classMessage",
                    classMessage.replace("alert-success", "alert-danger"));
            redirectAttributes.addFlashAttribute("messageTextMain", messageTextMain);
            redirectAttributes.addFlashAttribute("messageText", messageText);
            return "redirect:/#booking";
        }

        if (checkIn.isAfter(checkOut) || checkIn.isBefore(LocalDate.now()) || checkOut.isBefore(LocalDate.now())) {
            messageTextMain = "Ошибка!";
            messageText = checkIn.isAfter(checkOut)
                    ? "Дата заезда должна быть ДО даты выезда. Пожалуйста, укажите заново"
                    : "Нельзя выбирать прошедшую дату";
            redirectAttributes.addFlashAttribute(
                    "classMessage",
                    classMessage.replace("alert-success", "alert-danger"));
            redirectAttributes.addFlashAttribute("messageTextMain", messageTextMain);
            redirectAttributes.addFlashAttribute("messageText", messageText);
            return "redirect:/#booking";
        }

        if (mobileNumber.length() < 10) {
            messageTextMain = "Ошибка!";
            messageText = "Укажите корректный номер телефона, чтобы мы могли связаться с Вами";
            redirectAttributes.addFlashAttribute(
                    "classMessage",
                    classMessage.replace("alert-success", "alert-danger"));
            redirectAttributes.addFlashAttribute("messageTextMain", messageTextMain);
            redirectAttributes.addFlashAttribute("messageText", messageText);

            return "redirect:/#booking";
        }

        redirectAttributes.addFlashAttribute("classMessage", classMessage);
        redirectAttributes.addFlashAttribute("messageTextMain", messageTextMain);
        redirectAttributes.addFlashAttribute("messageText", messageText);
        double sumOfOrder = 0.0;
        for (var index = booking.getCheckIn(); index.isBefore(booking.getCheckOut()); index = index.plusDays(1)) {
            sumOfOrder += Double.parseDouble(calendar.get(index.format(DTF)).get("price"));
        }
        stringMessage += booking.getCheckIn().format(DTF)
                + " | "
                + booking.getCheckOut().format(DTF)
                + " | "
                + booking.getCountGuests()
                + " | "
                + sumOfOrder
                + " | "
                + booking.getMobileNumber();

        String finalStringMessage = stringMessage;
        users.forEach(user -> telegramBotService.sendMessage(user.getChatId(), finalStringMessage));
        log.warn(finalStringMessage);
        return "redirect:/#booking";
    }
}
