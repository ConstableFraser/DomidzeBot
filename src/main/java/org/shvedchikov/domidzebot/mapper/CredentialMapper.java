package org.shvedchikov.domidzebot.mapper;

import org.mapstruct.BeforeMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.ReportingPolicy;
import static org.shvedchikov.domidzebot.util.CoderDecoder.getCoderDecoder;
import org.shvedchikov.domidzebot.dto.credential.CredentialCreateDTO;
import org.shvedchikov.domidzebot.dto.credential.CredentialDTO;
import org.shvedchikov.domidzebot.dto.credential.CredentialUpdateDTO;
import org.shvedchikov.domidzebot.model.Credential;

@Mapper(
        uses = {JsonNullableMapper.class, ReferenceMapper.class},
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE,
        componentModel = MappingConstants.ComponentModel.SPRING,
        unmappedTargetPolicy = ReportingPolicy.IGNORE
)
public abstract class CredentialMapper {

    @Mapping(target = "password", source = "password")
    public abstract Credential map(CredentialCreateDTO model);

    public abstract CredentialDTO map(Credential model);

    //@Mapping(target = "login", source = "login")
    public abstract void update(CredentialUpdateDTO update, @MappingTarget Credential destination);

    @BeforeMapping
    public void encryptPassword(CredentialCreateDTO data) {
        var password = data.getPassword();
        try {
            data.setPassword(getCoderDecoder().encodePwd(password));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @BeforeMapping
    public void decryptPassword(Credential data) {
        var password = data.getPassword();
        try {
            data.setPassword(getCoderDecoder().decodePwd(password));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
