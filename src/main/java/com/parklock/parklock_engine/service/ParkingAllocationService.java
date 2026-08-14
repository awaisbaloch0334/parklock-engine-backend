package com.parklock.parklock_engine.service;

import com.parklock.parklock_engine.model.ParkingSpot;
import com.parklock.parklock_engine.model.ParkingTicket;
import com.parklock.parklock_engine.model.SpotStatus;
import com.parklock.parklock_engine.repository.ParkingSpotRepository;
import com.parklock.parklock_engine.repository.ParkingTicketRepository;
import com.parklock.parklock_engine.vehicle.Vehicle;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ParkingAllocationService {

    private final ParkingSpotRepository parkingSpotRepository;
    private final ParkingTicketRepository parkingTicketRepository;

    /*
     * INTERVIEW DEFENSE POINT (Distributed Pessimistic Locking):
     * We replaced JVM-level ReentrantLocks with Database-level Pessimistic Locks.
     * This ensures thread safety across multiple server instances in cloud environments.
     * The fallback loop guarantees high availability without blocking the entire database table.
     */
    @Transactional
    public ParkingTicket allocateSpotWithLock(Vehicle vehicle, String gateName) {
        
        // 1. Fetch available spots using an UNLOCKED snapshot read
        List<ParkingSpot> availableSpots = parkingSpotRepository.findByStatusAndSpotType(
                SpotStatus.AVAILABLE, 
                vehicle.getRequiredSpotType()
        );

        if (availableSpots.isEmpty()) {
            throw new IllegalStateException("No available spot found for type: " + vehicle.getRequiredSpotType());
        }

        // 2. Iterate through the snapshot and attempt to lock a single spot at the DB level
        for (ParkingSpot potentialSpot : availableSpots) {
            
            // 3. Acquire the database lock for this specific row
            ParkingSpot lockedSpot = parkingSpotRepository.findByIdWithLock(potentialSpot.getId())
                    .orElseThrow(() -> new IllegalStateException("Spot not found in database."));

            // 4. Double-check status after acquiring the lock
            if (lockedSpot.getStatus() == SpotStatus.AVAILABLE) {
                
                // We successfully claimed this spot! Mark it occupied and save.
                lockedSpot.setStatus(SpotStatus.OCCUPIED);
                parkingSpotRepository.save(lockedSpot);

                // 5. Generate and return the entry ticket
                ParkingTicket ticket = new ParkingTicket(
                        null,
                        UUID.randomUUID().toString(),
                        lockedSpot.getId(),
                        vehicle.getLicensePlate(),
                        vehicle.getRequiredSpotType(),
                        LocalDateTime.now(),
                        null,
                        null
                );

                return parkingTicketRepository.save(ticket);
            }
            
            // If the status was OCCUPIED, the loop gracefully continues to the next spot
        }

        // 6. If the loop finishes, heavy traffic grabbed all spots in our snapshot
        throw new IllegalStateException("Heavy traffic: All available spots were taken during booking. Please try again.");
    }
}