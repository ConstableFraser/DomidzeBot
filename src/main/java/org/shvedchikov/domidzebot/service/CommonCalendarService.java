package org.shvedchikov.domidzebot.service;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.shvedchikov.domidzebot.config.BotConfig;
import org.shvedchikov.domidzebot.dto.booking.BookingDTO;
import org.shvedchikov.domidzebot.dto.booking.BookingCreateDTO;
import org.shvedchikov.domidzebot.exception.ResourceNotFoundException;
import org.shvedchikov.domidzebot.repository.UserRepository;
import org.shvedchikov.domidzebot.util.Status;
import org.springframework.stereotype.Service;
import org.springframework.context.annotation.Lazy;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

@Slf4j
@Service
public class CommonCalendarService {
    private final TelegramBotService telegramBotService;
    private final UserRepository userRepository;
    private final BookingService bookingService;
    private final OrderService orderService;
    private final BotConfig botConfig;

    @Getter
    private Map<String, Map<String, String>> commonCalendar = new HashMap<>();

    private static final String NO_ORDERS = "Информация о бронях отсутствует";
    private static int amountDays;
    private static final DateTimeFormatter DTF = DateTimeFormatter.ofPattern("dd.MM.yyyy");

    public CommonCalendarService(
            @Lazy TelegramBotService telegramBotService,
            UserRepository userRepository,
            BookingService bookingService,
            OrderService orderService,
            BotConfig botConfig) {
        this.telegramBotService = telegramBotService;
        this.bookingService = bookingService;
        this.userRepository = userRepository;
        this.orderService = orderService;
        this.botConfig = botConfig;
        amountDays = LocalDate.MAX.lengthOfYear() * botConfig.getIndex();
    }

    public LinkedList<LinkedHashMap<String, String>> getTableOfCalendar() {
        var listDays = new LinkedList<LinkedHashMap<String, String>>();
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd.MM.yyyy");
        final int daysOfWeek = 7;
        var sequenceDay = LocalDate.now();

        while (sequenceDay.getDayOfWeek() != DayOfWeek.MONDAY) {
            sequenceDay = sequenceDay.minusDays(1);
        }

        for (int i = 0; i < amountDays;) {
            var map = new LinkedHashMap<String, String>();

            for (int j = 0; j < daysOfWeek && i <= amountDays; j++) {
                var date = sequenceDay.format(dtf);
                var price = Objects.equals(commonCalendar.getOrDefault(date, Map.of("channel", "default")).
                        get("channel"), "") ? commonCalendar.get(date).get("price") : "";
                map.put(date, price);

                sequenceDay = sequenceDay.plusDays(1);
                i++;
            }
            listDays.add(map);
        }
        return listDays;
    }

    public void calculateCalendar(Update update) {
        calculateCalendar(update, LocalDate.now().minusDays(amountDays), LocalDate.now().plusDays(amountDays));
    }

    private void calculateCalendar(Update update, LocalDate beginDate, LocalDate endDate) {
        var idCurrent = update.getMessage().getFrom().getId();
        if (!botConfig.isAdmin(idCurrent)) {
            log.warn("You are not an Admin. Id: {}", idCurrent);
            return;
        }
        var ordersSite = getOrdersOfSite(beginDate, endDate);
        var keySetSite = ordersSite.keySet();
        var ordersEthnomir = getOrdersOfEthnomir(beginDate, endDate);
        var keySetEthnomir = ordersEthnomir.keySet();

        for (String key : keySetSite) {
            if (keySetEthnomir.contains(key)) {
                log.warn("Внимание! Овербукинг на дату: {}", key);
                telegramBotService.sendMessage(
                        update.getMessage().getChat().getId(),
                        "Внимание! Овербукинг на дату: " + key
                );
            }
        }
        commonCalendar.putAll(ordersSite);
        commonCalendar.putAll(ordersEthnomir);
        for (var date = LocalDate.now().minusDays(amountDays);
             date.isBefore(LocalDate.now().plusDays(amountDays + 1));
             date = date.plusDays(1)) {
            commonCalendar.putIfAbsent(date.format(DTF), Map.of(
                    "checkin", "",
                    "checkout", "",
                    "countguests", "",
                    "telephone", "",
                    "program", "",
                    "price", "пусто",
                    "channel", ""
            ));
        }
        telegramBotService.sendMessage(update.getMessage().getChatId(), "Инициализация выполнена");
    }

    private Map<String, Map<String, String>> getOrdersOfSite(LocalDate beginDate, LocalDate endDate) {
        HashMap<String, Map<String, String>> siteCalendar = new HashMap<>();
        var ordersSite = bookingService.findByCheckInIsBetween(beginDate, endDate);

        for (BookingDTO booking : ordersSite) {
            var checkIn = booking.getCheckIn();
            var checkOut = booking.getCheckOut();

            while (checkIn.isBefore(checkOut)) {
                LocalDate finalCheckIn = checkIn;
                siteCalendar.put(
                        checkIn.format(DTF),
                        new LinkedHashMap<>() {
                            {
                                put("checkin", finalCheckIn.format(DTF));
                                put("checkout", booking.getCheckOut().format(DTF));
                                put("countguests", booking.getCountGuests().toString());
                                put("telephone", booking.getMobileNumber());
                                put("program", booking.getProgram());
                                put("price", booking.getPrice().toString());
                                put("channel", "site");
                            }
                        }
                );
                checkIn = checkIn.plusDays(1);
            }
        }
        return siteCalendar;
    }

    private Map<String, Map<String, String>> getOrdersOfEthnomir(LocalDate beginDate, LocalDate endDate) {
        String[] patterns = {"(?m)^\\d+\\s+\\|", "(?m)^\\n", "(?m)^Итого:.*", "\\x20", "(?m)\\|$"};
        var user = userRepository.findByUserTelegramId(botConfig.getIdAdmin())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        var parsedData = orderService.getOrders(user.getId(), beginDate, endDate);

        if (parsedData.equals(NO_ORDERS) || parsedData.isEmpty()) {
            return Collections.emptyMap();
        }

        for (String pattern : patterns) {
            parsedData = parsedData.replaceAll(pattern, "");
        }
        String[] s = parsedData.split("\\n");
        String[][] array = new String[s.length][s[0].length() * 2];

        for (int i = 0; i < s.length; i++) {
            array[i] = s[i].split("\\|");
        }

        Map<String, Map<String, String>> result = new HashMap<>();

        for (String[] line : array) {
            var checkout = LocalDate.parse(findCheckout(array, line[1]), DTF);
            result.put(line[0], new LinkedHashMap<>() {
                        {
                            put("checkin", LocalDate.parse(line[0], DTF).format(DTF));
                            put("checkout", checkout.format(DTF));
                            put("countguests", "");
                            put("telephone", "");
                            put("program", line[1]);
                            put("price", line[2]);
                            put("channel", "ethnomir");
                        }
                    }
            );
        }
        return result;
    }

    public String findCheckout(String[][] array, String program) {
        String checkout = "";
        for (String[] line : array) {
            if (line[1].equals(program)) {
                checkout = line[0];
            }
        }
        return LocalDate.parse(checkout, DTF).plusDays(1).format(DTF);
    }

    public void onSetPrice(Update update) {
        var textMessage = """
                Установка стоимость брони. Одну или несколько.
                Укажите в формате:

                <дата>|<цена>

                пример:
                12.01.2026|26600
                12.02.2026|31200
                """;
        telegramBotService.setStatus(Status.SETPRICE);
        telegramBotService.sendMessage(update.getMessage().getChatId(), textMessage);
    }

    public Status setPrice(TelegramBotService telegramBotService, Update update) {
        var idCurrent = update.getMessage().getFrom().getId();
        if (!botConfig.isAdmin(idCurrent)) {
            log.warn("You are not a Admin. Id: {}", idCurrent);
            return Status.DEFAULT;
        }
        String[] prices = update.getMessage().getText().split("\n");
        for (String price : prices) {
            String[] priceParsed = price.split("\\|");
            var date = LocalDate.parse(priceParsed[0], DTF);
            if (date.isBefore(LocalDate.now().plusDays(1))) {
                telegramBotService.sendMessage(
                        update.getMessage().getChatId(),
                        "Ошибка в данных: " + price + "\nзапустите команду заново");
                return Status.DEFAULT;
            }
            if (!commonCalendar.containsKey(priceParsed[0])) {
                telegramBotService.sendMessage(
                        update.getMessage().getChatId(),
                        "В календаре нет даты: " + price + "\nзапустите команду заново");
                return Status.DEFAULT;
            }
            var booking = commonCalendar.get(priceParsed[0]);
            if (!booking.get("channel").isEmpty()) {
                telegramBotService.sendMessage(
                        update.getMessage().getChatId(),
                        "На дату уже есть бронь: " + price + "\nзапустите команду заново");
                return Status.DEFAULT;
            }
            if (priceParsed[1].replaceAll("[a-zA-Z]", "").length() != priceParsed[1].length()) {
                telegramBotService.sendMessage(
                        update.getMessage().getChatId(),
                        "Ошибка в цене: " + price + "\nзапустите команду заново");
                return Status.DEFAULT;
            }
            commonCalendar.put(priceParsed[0], Map.of(
                    "checkin", priceParsed[0],
                    "checkout", "",
                    "countguests", "",
                    "telephone", "",
                    "program", "",
                    "price", priceParsed[1],
                    "channel", ""
            ));
        }
        telegramBotService.sendMessage(update.getMessage().getChatId(), "Цены установлены");
        return Status.DEFAULT;
    }

    public void setPriceFromBooking(BookingCreateDTO bookingCreateDTO) {
            final LocalDate date = bookingCreateDTO.getCheckIn();
            commonCalendar.put(date.format(DTF),
                    new LinkedHashMap<>() {
                        {
                            put("checkin", date.format(DTF));
                            put("checkout", bookingCreateDTO.getCheckOut().format(DTF));
                            put("countguests", String.valueOf(bookingCreateDTO.getCountGuests()));
                            put("telephone", bookingCreateDTO.getMobileNumber());
                            put("program", bookingCreateDTO.getProgram());
                            put("price", String.valueOf(bookingCreateDTO.getPrice()));
                            put("channel", "site");
                        }
                    }
            );
    }

    public void update(Update update) {
        calculateCalendar(update, LocalDate.now(), LocalDate.now().plusDays(amountDays));
    }

    public String getReportOfOrders(LocalDate start, LocalDate end, boolean withPrice) {
        StringBuilder report = new StringBuilder();
        Set<String> channelSite = new HashSet<>();
        Set<String> channelEthnomir = new HashSet<>();
        List<LocalDate> dates = new ArrayList<>();

        while (start.isBefore(end)) {
            Map<String, String> booking;
            if (commonCalendar.get(start.format(DTF)) == null
                    || commonCalendar.get(start.format(DTF)).get("channel").isEmpty()) {
                start = start.plusDays(1);
                continue;
            }
            booking = commonCalendar.get(start.format(DTF));

            for (Map.Entry<String, String> entry : booking.entrySet()) {
                if (!withPrice && (entry.getKey().equals("price") || entry.getKey().equals("telephone"))) {
                    continue;
                }
                report.append(entry.getValue());
                report.append("|");
            }

            switch (booking.get("channel")) {
                case "site" -> channelSite.add(booking.get("program"));
                case "ethnomir" -> channelEthnomir.add(booking.get("program"));
                default -> throw new IllegalArgumentException("the channel is not specified");
            }

            report.append("\n");
            dates.add(start);
            start = start.plusDays(1);
        }
        report.append("\n");
        report.append("Кол-во броней этномира: ").append(channelEthnomir.size());
        report.append("\n");
        report.append("Кол-во броней сайта: ").append(channelSite.size());
        if (withPrice) {
            report.append("\n").append("Сумма (этномир): ");
            report.append(getSumOfChannel(dates, "ethnomir"));

            report.append("\n").append("Сумма (сайт): ");
            report.append(getSumOfChannel(dates, "site"));
        }
        return report.toString();
    }

    private Double getSumOfChannel(List<LocalDate> dates, String channel) {
        return commonCalendar.entrySet().stream()
                .filter(entry -> dates.contains(LocalDate.parse(entry.getKey(), DTF)))
                .filter(entry -> entry.getValue().get("channel").equals(channel))
                .mapToDouble(entry -> Double.parseDouble(entry.getValue().get("price")))
                .sum();
    }

    public Integer getCountOfBookings(LocalDate start, LocalDate end) {
        int count = 0;
        for (int i = 0; start.plusDays(i).isBefore(end); i++) {
            count += commonCalendar.get(start.plusDays(i).format(DTF)) == null
                    || commonCalendar.get(start.plusDays(i).format(DTF)).get("channel").isEmpty() ? 0 : 1;
        }
        return count;
    }
}
