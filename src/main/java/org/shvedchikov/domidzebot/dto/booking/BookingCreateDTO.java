package org.shvedchikov.domidzebot.dto.booking;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Future;
import lombok.Getter;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

@Getter
@Setter
public class BookingCreateDTO {
    @DateTimeFormat(pattern = "dd.MM.yyyy")
    @Future
    private LocalDate checkIn;

    @DateTimeFormat(pattern = "dd.MM.yyyy")
    @Future
    private LocalDate checkOut;

    @Min(1)
    @Max(100)
    private Long countGuests;

    @Size(max = 100)
    private String mobileNumber;

    @NotNull
    private Double price;

    @NotNull
    @NotBlank
    private String program;

    @NotNull
    @JsonProperty("house_id")
    private Long houseId;
}
