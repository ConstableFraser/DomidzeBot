package org.shvedchikov.domidzebot.dto.booking;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.openapitools.jackson.nullable.JsonNullable;

import java.time.LocalDate;

@Getter
@Setter
public class BookingUpdateDTO {
    @NotNull
    @Future
    private JsonNullable<LocalDate> checkIn;

    @NotNull
    @Future
    private JsonNullable<LocalDate> checkOut;

    @NotNull
    @Min(1)
    @Max(100)
    private JsonNullable<Long> countGuests;

    @NotNull
    @NotBlank
    @Size(max = 100)
    private JsonNullable<String> mobileNumber;

    @NotNull
    private JsonNullable<Double> price;

    @NotNull
    @NotBlank
    private JsonNullable<String> program;

    @JsonProperty("house_id")
    private JsonNullable<Long> houseId;
}
