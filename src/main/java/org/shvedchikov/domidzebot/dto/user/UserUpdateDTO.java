package org.shvedchikov.domidzebot.dto.user;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.openapitools.jackson.nullable.JsonNullable;

@Setter
@Getter
public class UserUpdateDTO {

    @Email
    @NotBlank
    private JsonNullable<String> email;

    @NotBlank
    private JsonNullable<String> firstName;

    @NotBlank
    private JsonNullable<String> lastName;

    @NotNull
    private JsonNullable<Long> chatId;

    @NotBlank
    @Size(min = 3)
    private JsonNullable<String> password;

    @NotBlank
    private JsonNullable<Boolean> isEnable;
}
