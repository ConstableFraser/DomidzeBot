package org.shvedchikov.domidzebot.mapper;

import org.mapstruct.BeforeMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.ReportingPolicy;

import org.shvedchikov.domidzebot.component.CoderDecoder;
import org.shvedchikov.domidzebot.dto.credential.CredentialCreateDTO;
import org.shvedchikov.domidzebot.dto.credential.CredentialDTO;
import org.shvedchikov.domidzebot.dto.credential.CredentialUpdateDTO;
import org.shvedchikov.domidzebot.model.Credential;
import org.springframework.beans.factory.annotation.Autowired;

@Mapper(
        uses = {JsonNullableMapper.class, ReferenceMapper.class},
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE,
        componentModel = MappingConstants.ComponentModel.SPRING,
        unmappedTargetPolicy = ReportingPolicy.IGNORE
)
public abstract class CredentialMapper {
    @Autowired
    private CoderDecoder coderDecoder;

    @Mapping(target = "password", source = "password")
    public abstract Credential map(CredentialCreateDTO model);

    public abstract CredentialDTO map(Credential model);

    public abstract void update(CredentialUpdateDTO update, @MappingTarget Credential destination);

    @BeforeMapping
    public void encryptPassword(CredentialCreateDTO data) {
        var password = data.getPassword();
        try {
            data.setPassword(coderDecoder.encodePwd(password));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @BeforeMapping
    public void decryptPassword(Credential data) {
        var password = data.getPassword();
        try {
            data.setPassword(coderDecoder.decodePwd(password));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
