package com.parklock.parklock_engine.repository;

import com.parklock.parklock_engine.model.ParkingTicket;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ParkingTicketRepository extends JpaRepository<ParkingTicket, Long> {

    // Retrieves a ticket by its unique UUID string for checkout
    Optional<ParkingTicket> findByTicketNumber(String ticketNumber);

    // Checks if a specific parking bay currently has an active car parked in it
    Optional<ParkingTicket> findBySpotIdAndExitTimeIsNull(Long spotId);
}