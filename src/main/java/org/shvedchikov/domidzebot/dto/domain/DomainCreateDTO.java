package org.shvedchikov.domidzebot.dto.domain;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DomainCreateDTO {

    @NotNull
    @Size(min = 2, message = "{size domain name too short}")
    @Size(max = 100, message = "{size domain name too long}")
    private String domain;
}
