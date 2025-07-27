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
import org.shvedchikov.domidzebot.model.User;
import org.shvedchikov.domidzebot.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

@Slf4j
@Service
public class OrderService {
    @Autowired
    private HouseRepository houseRepository;

    @Autowired
    private RestRequestSender restRequestSender;

    @Autowired
    private CoderDecoder coderDecoder;

    @Autowired
    private UserRepository userRepository;

    private String login;
    private byte[] password;
    private User user;
    private static final String SEPARATOR = "-";
    private static final String ERRORMESSAGE = "В датах допущена ошибка.\nПопробуйте ещё раз";

    protected Status getDates(TelegramBotService telegramBotService, Update update) {
        telegramBotService.setStatus(Status.SETPERIOD);
        var user = userRepository.findByUserTelegramId(update.getMessage().getFrom().getId());
        var chatId = update.getMessage().getChatId();
        if (user.isEmpty() || !user.get().isEnabled()) {
            log.warn("Attempt to request period: {}", update.getMessage().getFrom().getId());
            telegramBotService.sendMessage(chatId, "Требуется регистрация");
            return Status.SETPERIOD;
        }
        this.user = user.get();
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
        var startDate = parseDate(dates[0].trim());
        var endDate = parseDate(dates[1].trim());

        if (startDate.isEmpty() || endDate.isEmpty() || startDate.get().isAfter(endDate.get())) {
            messageUp(telegramBotService, update);
            return Status.SETPERIOD;
        }
        telegramBotService.sendMessage(update.getMessage().getChatId(),
                getOrders(user, startDate.get(), endDate.get().plusDays(1), true));
        telegramBotService.setFunc(telegramBotService.getStatus(), this::getDates);
        return Status.DEFAULT;
    }

    private void messageUp(TelegramBotService telegramBotService, Update update) {
        telegramBotService.sendMessage(update.getMessage().getChatId(), ERRORMESSAGE);
        telegramBotService.setFunc(telegramBotService.getStatus(), this::setDates);
    }

    private Optional<LocalDate> parseDate(String date) {
        var formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy");
        try {
            return Optional.of(LocalDate.parse(date, formatter));
        } catch (DateTimeParseException ignored) {
            log.warn("User date {} is not valid", date);
            return Optional.empty();
        }
    }

    protected String getOrders(User user, LocalDate startDate, LocalDate endDate, Boolean withPrice) {
        var result = houseRepository.findAllByOwner(user.getId());
        // domain = String.valueOf(result.get(0).getOrDefault("domain", "null"));
        login = String.valueOf(result.get(0).getOrDefault("login", "null"));
        var pwd = String.valueOf(result.get(0).getOrDefault("pwd", "null"));

        try {
            password = coderDecoder.decodePwd(pwd).getBytes(StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return parserHtml(connectedToDomain(startDate, endDate), withPrice);
    }

    private String connectedToDomain(LocalDate startDate, LocalDate endDate) {
        restRequestSender.setHost("https://ethnomir.ru/personal/owner/");
        restRequestSender.setHeaders(login, new String(password, StandardCharsets.UTF_8), startDate, endDate);
        return restRequestSender.sendRequest();
    }

    private String parserHtml(String body, Boolean withPrice) {
        int indexOrder = 4;
        var regStr1 = "\\* Цена до вычета услуг управляющего агента. \\| ";
        var regStr2 = "Дома \\| Барн Хаус \\d+ \\| ";
        StringBuilder result = new StringBuilder();
        Set<String> countOrders = new HashSet<>();
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
            if (!withPrice && !iterRows.hasNext()) {
                break;
            }
            Elements cells = iterRow.select("th, td");
            Iterator<Element> iterCells = cells.iterator();
            if (cells.size() > indexOrder) {
                countOrders.add(cells.get(indexOrder).text());
            }
            while (iterCells.hasNext()) {
                var element = iterCells.next();
                if (!withPrice && !iterCells.hasNext()) {
                    break;
                }
                var text = element.text();
                result.append(text).append(" | ");
            }
            var str = iterRows.hasNext() ? "\n" : "";
            result.append(str);
        }
        result.append("Броней: ").append(countOrders.size());
        return result.toString().replaceAll(regStr1, "\n").replaceAll(regStr2, "");
    }
}
