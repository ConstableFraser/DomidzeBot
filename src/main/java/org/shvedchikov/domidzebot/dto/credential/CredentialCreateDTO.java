package org.shvedchikov.domidzebot.dto.credential;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CredentialCreateDTO {
    private String login;
    private String password;
}
