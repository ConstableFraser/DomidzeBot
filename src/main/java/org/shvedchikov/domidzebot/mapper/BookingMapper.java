package org.shvedchikov.domidzebot.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.ReportingPolicy;
import org.shvedchikov.domidzebot.dto.booking.BookingCreateDTO;
import org.shvedchikov.domidzebot.dto.booking.BookingDTO;
import org.shvedchikov.domidzebot.dto.booking.BookingUpdateDTO;
import org.shvedchikov.domidzebot.exception.ResourceNotFoundException;
import org.shvedchikov.domidzebot.model.Booking;
import org.shvedchikov.domidzebot.model.House;
import org.shvedchikov.domidzebot.repository.HouseRepository;
import org.springframework.beans.factory.annotation.Autowired;

@Mapper(
        uses = {JsonNullableMapper.class, ReferenceMapper.class},
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE,
        componentModel = MappingConstants.ComponentModel.SPRING,
        unmappedTargetPolicy = ReportingPolicy.IGNORE
)
public abstract class BookingMapper {
    @Autowired
    private HouseRepository houseRepository;

    @Mapping(target = "house", source = "houseId")
    public abstract Booking map(BookingCreateDTO model);

    @Mapping(target = "houseId", source = "house")
    public abstract BookingDTO map(Booking model);

    @Mapping(target = "house", source = "houseId")
    public abstract void update(BookingUpdateDTO bookingUpdateDTO, @MappingTarget Booking destination);

    public House mapIdToHouse(Long id) {
        return houseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("House hot found"));
    }

    public Long mapHouseToId(House house) {
        return house.getId();
    }
}
