package org.shvedchikov.domidzebot.dto.house;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class HouseCreateDTO {
    @NotNull
    private Integer number;

    @NotNull
    @JsonProperty("owner_id")
    private Long ownerId;
}
