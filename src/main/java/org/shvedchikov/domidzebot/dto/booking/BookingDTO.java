package org.shvedchikov.domidzebot.dto.booking;

import lombok.Getter;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

@Getter
@Setter
public class BookingDTO {
    private Long id;
    @DateTimeFormat(pattern = "dd.MM.yyyy")
    private LocalDate checkIn;
    @DateTimeFormat(pattern = "dd.MM.yyyy")
    private LocalDate checkOut;
    private Long countGuests;
    private String mobileNumber;
    private Double price;
    private String program;
    private Long houseId;
}
