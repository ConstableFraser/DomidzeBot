package org.shvedchikov.domidzebot.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.ReportingPolicy;
import org.shvedchikov.domidzebot.dto.domain.DomainCreateDTO;
import org.shvedchikov.domidzebot.dto.domain.DomainDTO;
import org.shvedchikov.domidzebot.dto.domain.DomainUpdateDTO;
import org.shvedchikov.domidzebot.model.Domain;

@Mapper(
        uses = {JsonNullableMapper.class, ReferenceMapper.class},
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE,
        componentModel = MappingConstants.ComponentModel.SPRING,
        unmappedTargetPolicy = ReportingPolicy.IGNORE
)
public abstract class DomainMapper {
    public abstract Domain map(DomainCreateDTO model);
    public abstract DomainDTO map(Domain model);
    public abstract void update(DomainUpdateDTO domainUpdateDTO, @MappingTarget Domain targetDomain);
}
