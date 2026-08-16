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
import java.util.Comparator; // Added import for sorting
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ParkingAllocationService {

    private final ParkingSpotRepository parkingSpotRepository;
    private final ParkingTicketRepository parkingTicketRepository;

    @Transactional
    public ParkingTicket allocateSpotWithLock(Vehicle vehicle, String gateName) {
        
        List<ParkingSpot> availableSpots = parkingSpotRepository.findByStatusAndSpotType(
                SpotStatus.AVAILABLE, 
                vehicle.getRequiredSpotType()
        );

        if (availableSpots.isEmpty()) {
            throw new IllegalStateException("No available spot found for type: " + vehicle.getRequiredSpotType());
        }

        // SORT SECURELY: Guarantee we always pick the lowest ID first (Sequential Order)
        availableSpots.sort(Comparator.comparing(ParkingSpot::getId));

        for (ParkingSpot potentialSpot : availableSpots) {
            
            ParkingSpot lockedSpot = parkingSpotRepository.findByIdWithLock(potentialSpot.getId())
                    .orElseThrow(() -> new IllegalStateException("Spot not found in database."));

            if (lockedSpot.getStatus() == SpotStatus.AVAILABLE) {
                
                lockedSpot.setStatus(SpotStatus.OCCUPIED);
                parkingSpotRepository.save(lockedSpot);

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
        }

        throw new IllegalStateException("Heavy traffic: All available spots were taken during booking. Please try again.");
    }
}