package org.shvedchikov.domidzebot.service;

import org.shvedchikov.domidzebot.util.Status;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.shvedchikov.domidzebot.component.CoderDecoder;
import org.shvedchikov.domidzebot.component.RestRequestSender;
import org.shvedchikov.domidzebot.repository.HouseRepository;
import org.shvedchikov.domidzebot.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.context.annotation.Lazy;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Iterator;
import java.util.Objects;

@Slf4j
@Service
public class OrderService {
    private final HouseRepository houseRepository;
    private final RestRequestSender restRequestSender;
    private final CoderDecoder coderDecoder;
    private final UserRepository userRepository;
    private final CommonCalendarService commonCalendarService;

    private static final String SEPARATOR = "-";
    private static final String ERRORMESSAGE = "В датах допущена ошибка.\nПопробуйте ещё раз";

    public OrderService(HouseRepository houseRepository,
                        RestRequestSender restRequestSender,
                        CoderDecoder coderDecoder,
                        UserRepository userRepository,
                        @Lazy CommonCalendarService commonCalendarService) {
        this.houseRepository = houseRepository;
        this.restRequestSender = restRequestSender;
        this.coderDecoder = coderDecoder;
        this.userRepository = userRepository;
        this.commonCalendarService = commonCalendarService;
    }

    protected Status getDates(TelegramBotService telegramBotService, Update update) {
        telegramBotService.setStatus(Status.SETPERIOD);
        var user = userRepository.findByUserTelegramId(update.getMessage().getFrom().getId());
        var chatId = update.getMessage().getChatId();
        if (user.isEmpty() || !user.get().isEnabled()) {
            log.warn("Attempt to request period: {}", update.getMessage().getFrom().getId());
            telegramBotService.sendMessage(chatId, "Требуется регистрация");
            return Status.DEFAULT;
        }
        telegramBotService.sendMessage(chatId, "Укажите даты через дефис, например: 02.04.2025-02.05.2025");
        telegramBotService.setFunc(telegramBotService.getStatus(), this::setDates);
        return Status.SETPERIOD;
    }

    protected Status setDates(TelegramBotService telegramBotService, Update update) {
        telegramBotService.setStatus(Status.SETPERIOD);
        var text = update.getMessage().getText();
        var index = text.indexOf(SEPARATOR);
        if (index == -1) {
            messageUp(telegramBotService, update);
            return Status.SETPERIOD;
        }
        String[] dates = text.split(SEPARATOR);
        var formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy");
        var startDate = LocalDate.parse(dates[0].trim(), formatter);
        var endDate = LocalDate.parse(dates[1].trim(), formatter);

        if (startDate.toString().isEmpty() || endDate.toString().isEmpty() || startDate.isAfter(endDate)) {
            messageUp(telegramBotService, update);
            return Status.SETPERIOD;
        }
        telegramBotService.sendMessage(update.getMessage().getChatId(),
                commonCalendarService.getReportOfOrders(startDate, endDate, true));
        telegramBotService.setFunc(telegramBotService.getStatus(), this::getDates);
        return Status.DEFAULT;
    }

    private void messageUp(TelegramBotService telegramBotService, Update update) {
        telegramBotService.sendMessage(update.getMessage().getChatId(), ERRORMESSAGE);
        telegramBotService.setFunc(telegramBotService.getStatus(), this::setDates);
    }

    public String getOrders(Long userId, LocalDate startDate, LocalDate endDate) {
        return parserHtml(connectedToDomain(userId, startDate, endDate));
    }

    public String connectedToDomain(Long userId, LocalDate startDate, LocalDate endDate) {
        var result = houseRepository.findAllByOwner(userId);
        // domain = String.valueOf(result.get(0).getOrDefault("domain", "null"));
        String login = String.valueOf(result.get(0).getOrDefault("login", "null"));
        var pwd = String.valueOf(result.get(0).getOrDefault("pwd", "null"));

        byte[] password;
        try {
            password = coderDecoder.decodePwd(pwd).getBytes(StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        restRequestSender.setHost("https://ethnomir.ru/personal/owner/");
        restRequestSender.setHeaders(login, new String(password, StandardCharsets.UTF_8), startDate, endDate);
        return restRequestSender.sendRequest();
    }

    private String parserHtml(String body) {
        var regStr1 = "\\* Цена до вычета услуг управляющего агента. \\| ";
        var regStr2 = "Дома \\| Барн Хаус \\d+ \\| ";
        StringBuilder result = new StringBuilder();
        Document document = Jsoup.parse(body);
        if (Objects.isNull(document)) {
            return "Информация о бронях отсутствует";
        }

        Element table = document.getElementById("sortable");
        if (Objects.isNull(table)) {
            return "Информация о бронях отсутствует";
        }
        Elements rows = table.select("tr");
        rows.remove(0);

        Iterator<Element> iterRows = rows.iterator();

        while (iterRows.hasNext()) {
            var iterRow = iterRows.next();
            Elements cells = iterRow.select("th, td");
            for (Element element : cells) {
                var text = element.text();
                result.append(text).append(" | ");
            }
            var str = iterRows.hasNext() ? "\n" : "";
            result.append(str);
        }
        return result.toString().replaceAll(regStr1, "\n").replaceAll(regStr2, "");
    }
}
