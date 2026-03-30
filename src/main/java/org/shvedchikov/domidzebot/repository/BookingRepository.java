package org.shvedchikov.domidzebot.repository;

import org.shvedchikov.domidzebot.model.Booking;
import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface BookingRepository extends JpaRepository<Booking, Long> {
    List<Booking> findByCheckInIsBetween(LocalDate checkInAfter, LocalDate checkInBefore, Limit limit);
}
