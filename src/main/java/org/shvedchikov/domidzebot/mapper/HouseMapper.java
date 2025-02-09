package org.shvedchikov.domidzebot.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.ReportingPolicy;
import org.shvedchikov.domidzebot.dto.house.HouseCreateDTO;
import org.shvedchikov.domidzebot.dto.house.HouseDTO;
import org.shvedchikov.domidzebot.dto.house.HouseUpdateDTO;
import org.shvedchikov.domidzebot.exception.ResourceNotFoundException;
import org.shvedchikov.domidzebot.model.Credential;
import org.shvedchikov.domidzebot.model.Domain;
import org.shvedchikov.domidzebot.model.House;
import org.shvedchikov.domidzebot.repository.CredentialRepository;
import org.shvedchikov.domidzebot.repository.DomainRepository;
import org.springframework.beans.factory.annotation.Autowired;

@Mapper(
        uses = {JsonNullableMapper.class, ReferenceMapper.class},
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE,
        componentModel = MappingConstants.ComponentModel.SPRING,
        unmappedTargetPolicy = ReportingPolicy.IGNORE
)
public abstract class HouseMapper {

    @Autowired
    private DomainRepository domainRepository;

    @Autowired
    private CredentialRepository credentialRepository;

    @Mapping(target = "owner", source = "ownerId")
    @Mapping(target = "domain", source = "domainId")
    @Mapping(target = "credential", source = "credentialId")
    public abstract House map(HouseCreateDTO model);

    @Mapping(target = "ownerId", source = "owner.id")
    @Mapping(target = "domainId", source = "domain.id")
    @Mapping(target = "credentialId", source = "credential.id")
    public abstract HouseDTO map(House model);

    @Mapping(target = "owner", source = "ownerId")
    @Mapping(target = "domain", source = "domainId")
    @Mapping(target = "credential", source = "credentialId")
    public abstract void update(HouseUpdateDTO houseUpdateDTO, @MappingTarget House targetHouse);

    public Domain mapIdToDomain(Long id) {
        return domainRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Domain hot found"));
    }

    public Credential mapIdToCredential(Long id) {
        return credentialRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Credential hot found"));
    }
}
