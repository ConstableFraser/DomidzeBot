package org.shvedchikov.domidzebot.dto.credential;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import org.openapitools.jackson.nullable.JsonNullable;

@Data
public class CredentialUpdateDTO {
    @NotBlank
    private JsonNullable<String> login;

    @NotBlank
    private JsonNullable<String> password;
}
