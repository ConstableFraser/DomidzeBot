package org.shvedchikov.domidzebot.dto.domain;

import lombok.Getter;
import lombok.Setter;
import org.openapitools.jackson.nullable.JsonNullable;

@Getter
@Setter
public class DomainUpdateDTO {
    private JsonNullable<String> domain;
}
