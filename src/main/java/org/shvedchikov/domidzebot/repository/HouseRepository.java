package org.shvedchikov.domidzebot.repository;

import org.shvedchikov.domidzebot.model.House;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface HouseRepository extends JpaRepository<House, Long> {
    Optional<House> findHouseByOwnerIdAndNumber(Long ownerId, Integer number);
    List<House> findAllByOwnerId(Long ownerId);
}
