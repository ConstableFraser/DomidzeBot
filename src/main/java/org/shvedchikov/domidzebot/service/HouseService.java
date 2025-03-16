package org.shvedchikov.domidzebot.service;

import jakarta.validation.Valid;
import org.shvedchikov.domidzebot.dto.house.HouseCreateDTO;
import org.shvedchikov.domidzebot.dto.house.HouseDTO;
import org.shvedchikov.domidzebot.dto.house.HouseUpdateDTO;
import org.shvedchikov.domidzebot.exception.ResourceNotFoundException;
import org.shvedchikov.domidzebot.mapper.HouseMapper;
import org.shvedchikov.domidzebot.repository.HouseRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;
import java.util.Objects;

@Service
public class HouseService {

    @Autowired
    private HouseRepository houseRepository;

    @Autowired
    private HouseMapper houseMapper;

    public List<HouseDTO> getAll() {
        var houses = houseRepository.findAll();
        return houses.stream()
                .map(houseMapper::map)
                .toList();
    }

    @Transactional
    public HouseDTO create(@Valid @RequestBody HouseCreateDTO houseCreateDTO) {
        var house = houseMapper.map(houseCreateDTO);
        houseRepository.save(house);

        return houseMapper.map(house);
    }

    @Transactional
    public HouseDTO update(@RequestBody HouseUpdateDTO houseUpdateDTO, @PathVariable Long id) {
        var house = houseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("House with id: " + id + " not found"));
        houseMapper.update(houseUpdateDTO, house);
        house = houseRepository.save(house);

        return houseMapper.map(house);
    }

    public HouseDTO show(Long id) {
        var house = houseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("House with id: " + id + " not found"));
        return houseMapper.map(house);
    }

    public List<HouseDTO> getByOwnerIdAndNumberHouse(@PathVariable Long ownerId, @PathVariable Integer houseNumber) {
        if (Objects.isNull(houseNumber)) {
            return houseRepository.findAllByOwnerId(ownerId).stream()
                    .map(houseMapper::map)
                    .toList();
        }
        return houseRepository.findAllByOwnerId(ownerId)
                .stream()
                .filter(h -> Objects.equals(h.getNumber(), houseNumber))
                .map(houseMapper::map)
                .toList();
    }

    @Transactional
    public void destroy(@PathVariable Long id) {
        houseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("House with id: " + id + " not found"));
        houseRepository.deleteById(id);
    }
}
