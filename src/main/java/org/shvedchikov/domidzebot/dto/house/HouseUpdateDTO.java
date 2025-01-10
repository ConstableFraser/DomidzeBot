package org.shvedchikov.domidzebot.dto.house;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;
import org.openapitools.jackson.nullable.JsonNullable;

@Getter
@Setter
public class HouseUpdateDTO {
    private JsonNullable<Integer> number;

    @JsonProperty("owner_id")
    private JsonNullable<Long> ownerId;
}
