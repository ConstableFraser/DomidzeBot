package org.shvedchikov.domidzebot.util;

import org.instancio.Instancio;
import org.instancio.Model;
import org.instancio.Select;
import org.shvedchikov.domidzebot.dto.booking.BookingCreateDTO;
import org.shvedchikov.domidzebot.dto.house.HouseCreateDTO;
import org.shvedchikov.domidzebot.model.Credential;
import org.shvedchikov.domidzebot.model.Domain;
import org.shvedchikov.domidzebot.model.User;
import org.springframework.stereotype.Component;

import net.datafaker.Faker;
import lombok.Getter;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

@Component
@Getter
public class ModelGenerator {

    private final Model<User> userModel;
    private final Model<HouseCreateDTO> houseModel;
    private final Model<Domain> domainModel;
    private final Model<Credential> domainCredential;
    private final Model<BookingCreateDTO> bookingModel;
    private final Faker faker;

    public ModelGenerator() {
        this.faker = new Faker( Locale.of("ru"));

        userModel = Instancio.of(User.class)
                .ignore(Select.field(User::getId))
                .supply(Select.field(User::getEmail), () -> faker.internet().emailAddress())
                .supply(Select.field(User::getFirstName), () -> faker.name().name().split(" ")[0])
                .supply(Select.field(User::getLastName), () -> faker.name().name().split(" ")[1])
                .toModel();

        houseModel = Instancio.of(HouseCreateDTO.class)
                .ignore(Select.field(HouseCreateDTO::getOwnerId))
                .supply(Select.field(HouseCreateDTO::getNumber), () -> 755)
                .toModel();

        domainModel = Instancio.of(Domain.class)
                .ignore(Select.field(Domain::getId))
                .supply(Select.field(Domain::getDomain), () -> faker.internet().webdomain())
                .toModel();

        domainCredential = Instancio.of(Credential.class)
                .ignore(Select.field(Credential::getId))
                .supply(Select.field(Credential::getLogin), () -> faker.internet().username())
                .supply(Select.field(Credential::getPassword), () -> faker.hacker().verb())
                .toModel();

        bookingModel = Instancio.of(BookingCreateDTO.class)
                .supply(Select.field(BookingCreateDTO::getCheckIn), () -> LocalDate.now().plusDays(1))
                .supply(Select.field(BookingCreateDTO::getCheckOut), () -> LocalDate.now().plusDays(2))
                .supply(Select.field(BookingCreateDTO::getPrice), () -> Double.valueOf(faker.number().numberBetween(14500, 99900)))
                .supply(Select.field(BookingCreateDTO::getCountGuests), () -> Long.valueOf(faker.number().numberBetween(1, 10)))
                .supply(Select.field(BookingCreateDTO::getMobileNumber), () -> faker.phoneNumber().phoneNumber())
                .supply(Select.field(BookingCreateDTO::getProgram), () -> String.valueOf(faker.number().randomNumber()))
                .toModel();
    }
}
