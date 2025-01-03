package org.shvedchikov.domidzebot.util;

import org.instancio.Instancio;
import org.instancio.Model;
import org.instancio.Select;
import org.shvedchikov.domidzebot.model.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import net.datafaker.Faker;
import lombok.Getter;

@Component
public class ModelGenerator {

    @Getter
    private Model<User> userModel;

    @Autowired
    private Faker faker;

    @PostConstruct
    private void init() {
        userModel = Instancio.of(User.class)
                .ignore(Select.field(User::getId))
                .supply(Select.field(User::getEmail), () -> faker.internet().emailAddress())
                .supply(Select.field(User::getFirstName), () -> faker.name().name().split(" ")[0])
                .supply(Select.field(User::getLastName), () -> faker.name().name().split(" ")[1])
                .toModel();
    }

}
