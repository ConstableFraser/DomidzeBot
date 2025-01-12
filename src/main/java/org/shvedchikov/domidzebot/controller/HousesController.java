package org.shvedchikov.domidzebot.controller;

import jakarta.validation.Valid;
import org.shvedchikov.domidzebot.dto.house.HouseCreateDTO;
import org.shvedchikov.domidzebot.dto.house.HouseDTO;
import org.shvedchikov.domidzebot.dto.house.HouseUpdateDTO;
import org.shvedchikov.domidzebot.service.HouseService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api")
public class HousesController {

    @Autowired
    private HouseService houseService;

    @GetMapping("/houses")
    @ResponseStatus(HttpStatus.OK)
    public ResponseEntity<List<HouseDTO>> index() {
        var houses = houseService.getAll();

        return ResponseEntity.ok()
                .header("X-Total-Count", String.valueOf(houses.size()))
                .body(houses);
    }

    @PostMapping("/houses")
    @ResponseStatus(HttpStatus.CREATED)
    public HouseDTO create(@Valid @RequestBody HouseCreateDTO houseCreateDTO) {
        return houseService.create(houseCreateDTO);
    }

    @GetMapping("/houses/{id}")
    @ResponseStatus(HttpStatus.OK)
    public HouseDTO show(@PathVariable Long id) {
        return houseService.show(id);
    }

    @GetMapping("/houses/filter")
    @ResponseStatus(HttpStatus.OK)
    public List<HouseDTO> showByOwnerIdAndNumberHouse(@RequestParam(name = "owner") Long ownerId,
                                  @RequestParam(name = "house", required = false) Integer houseNumber) {
        return houseService.getByOwnerIdAndNumberHouse(ownerId, houseNumber);
    }

    @PutMapping("/houses/{id}")
    @ResponseStatus(HttpStatus.OK)
    public HouseDTO update(@RequestBody HouseUpdateDTO houseData, @PathVariable Long id) {
        return houseService.update(houseData, id);
    }

    @DeleteMapping("/houses/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void destroy(@PathVariable Long id) {
        houseService.destroy(id);
    }
}
