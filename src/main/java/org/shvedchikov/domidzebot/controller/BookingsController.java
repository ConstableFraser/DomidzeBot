package org.shvedchikov.domidzebot.controller;

import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.shvedchikov.domidzebot.dto.booking.BookingCreateDTO;
import org.shvedchikov.domidzebot.dto.booking.BookingDTO;
import org.shvedchikov.domidzebot.dto.booking.BookingUpdateDTO;
import org.shvedchikov.domidzebot.service.BookingService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api")
public class BookingsController {
    private final BookingService bookingService;
    private static final DateTimeFormatter DTF = DateTimeFormatter.ofPattern("dd.MM.yyyy");

    public BookingsController(BookingService bookingService) {
        this.bookingService = bookingService;
    }

    @GetMapping("/bookings")
    @ResponseStatus(HttpStatus.OK)
    public ResponseEntity<List<BookingDTO>> getBookings(@RequestParam String checkInAfter,
                                                        @RequestParam String checkInBefore) {
        var dateBegin = LocalDate.parse(checkInAfter, DTF);
        var dateEnd = LocalDate.parse(checkInBefore, DTF);
        var bookings = bookingService.findByCheckInIsBetween(dateBegin, dateEnd);

        return ResponseEntity.ok()
                .header("X-Total-Count", String.valueOf(bookings.size()))
                .body(bookings);
    }

    @PostMapping("/bookings")
    @ResponseStatus(HttpStatus.CREATED)
    public BookingDTO booking(@Valid @RequestBody BookingCreateDTO booking) {
        return bookingService.create(booking);
    }

    @GetMapping("/bookings/{id}")
    @ResponseStatus(HttpStatus.OK)
    public BookingDTO show(@PathVariable Long id) {
        return bookingService.show(id);
    }

    @PutMapping("/bookings/{id}")
    @ResponseStatus(HttpStatus.OK)
    public BookingDTO update(@Valid @RequestBody BookingUpdateDTO bookingData, @PathVariable Long id) {
        return bookingService.update(bookingData, id);
    }

    @DeleteMapping("/bookings/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void destroy(@PathVariable Long id) {
        bookingService.destroy(id);
    }
}
