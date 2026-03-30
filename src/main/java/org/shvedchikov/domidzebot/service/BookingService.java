package org.shvedchikov.domidzebot.service;

import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.shvedchikov.domidzebot.config.BotConfig;
import org.shvedchikov.domidzebot.dto.booking.BookingCreateDTO;
import org.shvedchikov.domidzebot.dto.booking.BookingDTO;
import org.shvedchikov.domidzebot.dto.booking.BookingUpdateDTO;
import org.shvedchikov.domidzebot.exception.ResourceNotFoundException;
import org.shvedchikov.domidzebot.mapper.BookingMapper;
import org.shvedchikov.domidzebot.model.Booking;
import org.shvedchikov.domidzebot.repository.BookingRepository;
import org.shvedchikov.domidzebot.repository.HouseRepository;
import org.shvedchikov.domidzebot.util.Status;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.domain.Limit;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Slf4j
@Service
public class BookingService {
    public static final Integer LIMIT = 2000;
    private static final DateTimeFormatter DTF = DateTimeFormatter.ofPattern("dd.MM.yyyy");

    private final HouseRepository houseRepository;
    private final TelegramBotService telegramBotService;
    private final CommonCalendarService commonCalendarService;
    private final BookingRepository bookingRepository;
    private final BookingMapper bookingMapper;
    private final BotConfig botConfig;

    public BookingService(HouseRepository houseRepository,
                          BookingRepository bookingRepository,
                          BookingMapper bookingMapper,
                          @Lazy TelegramBotService telegramBotService,
                          @Lazy CommonCalendarService commonCalendarService,
                          BotConfig botConfig) {
        this.houseRepository = houseRepository;
        this.bookingRepository = bookingRepository;
        this.bookingMapper = bookingMapper;
        this.telegramBotService = telegramBotService;
        this.commonCalendarService = commonCalendarService;
        this.botConfig = botConfig;
    }

    public BookingDTO create(@Valid @RequestBody BookingCreateDTO bookingCreateDTO) {
        Booking booking = bookingMapper.map(bookingCreateDTO);
        bookingRepository.save(booking);
        return bookingMapper.map(booking);
    }

    public BookingDTO show(Long id) {
        Booking booking = bookingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Booking  with id: " + id + " not found"));
        return bookingMapper.map(booking);
    }

    public BookingDTO update(@RequestBody BookingUpdateDTO bookingUpdateDTO, @PathVariable Long id) {
        Booking booking = bookingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Booking  with id: " + id + " not found"));
        bookingMapper.update(bookingUpdateDTO, booking);
        bookingRepository.save(booking);
        return bookingMapper.map(booking);
    }

    public void destroy(@PathVariable Long id) {
        bookingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Booking  with id: " + id + " not found"));
        bookingRepository.deleteById(id);
    }

    public List<BookingDTO> findByCheckInIsBetween(LocalDate checkInAfter, LocalDate checkInBefore) {
        var bookings = bookingRepository.findByCheckInIsBetween(checkInAfter, checkInBefore, Limit.of(LIMIT));
        return bookings.stream()
                .map(bookingMapper::map)
                .toList();
    }

    public void onSetBooking(Update update) {
        var textMessage = """
                Установка брони с сайта. Укажите в формате:

                <дата заезда>|<программа>|<гостей>|<телефон>|<цена>|<номер дома>

                пример:
                12.01.2026|2203/26|5|+79991112223|2600|755
                13.01.2026|2203/26|5|+79991112223|1600|755""";
        telegramBotService.setStatus(Status.SETBOOKING);
        telegramBotService.sendMessage(update.getMessage().getChatId(), textMessage);
    }

    public Status onGetBooking(TelegramBotService telegramBotService, Update update) {
        var indexProgram = 1;
        var idCurrent = update.getMessage().getFrom().getId();
        if (!botConfig.isAdmin(idCurrent)) {
            log.warn("You are not a Admin. Id: {}", idCurrent);
            return Status.DEFAULT;
        }

        String[] message = update.getMessage().getText().split("\\n");
        String[][] listBooking = new String[message.length][message[0].length() * 2];

        for (int i = 0; i < message.length; i++) {
            listBooking[i] = message[i].split("\\|");
        }

        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd.MM.yyyy");

        for (String[] booking : listBooking) {
            var checkout = LocalDate.parse(commonCalendarService.findCheckout(
                    listBooking,
                    booking[indexProgram]), DTF);
            var start = LocalDate.parse(booking[0].trim(), dtf);
            while (start.isBefore(checkout)) {
                if (!commonCalendarService.getCommonCalendar().get(start.format(DTF)).get("channel").isEmpty()) {
                    telegramBotService.sendMessage(update.getMessage().getChatId(),
                            start.format(DTF) + " - дата уже занята");
                    return Status.DEFAULT;
                }
                start = start.plusDays(1);
            }

            BookingCreateDTO bookingCreateDTO = new BookingCreateDTO();
            bookingCreateDTO.setCheckIn(LocalDate.parse(booking[0].trim(), dtf));
            bookingCreateDTO.setCheckOut(checkout);
            bookingCreateDTO.setCountGuests(Long.parseLong(booking[2].trim()));
            bookingCreateDTO.setMobileNumber(booking[3].trim());
            bookingCreateDTO.setPrice(Double.parseDouble(booking[4].trim()));
            bookingCreateDTO.setHouseId(houseRepository.findHouseByNumber(Integer.parseInt(booking[5].trim())).getId());
            bookingCreateDTO.setProgram(booking[1].trim());

            var textMessage = create(bookingCreateDTO) != null ? "Заказ сохранён успешно!" : "Возникла ошибка";
            telegramBotService.sendMessage(update.getMessage().getChatId(), textMessage);
            commonCalendarService.setPriceFromBooking(bookingCreateDTO);
        }
        return Status.DEFAULT;
    }
}
