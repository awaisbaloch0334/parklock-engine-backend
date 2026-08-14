package com.parklock.parklock_engine.service;

import com.parklock.parklock_engine.model.AuditAction;
import com.parklock.parklock_engine.model.AuditLog;
import com.parklock.parklock_engine.model.ParkingSpot;
import com.parklock.parklock_engine.model.ParkingTicket;
import com.parklock.parklock_engine.model.SpotStatus;
import com.parklock.parklock_engine.repository.AuditLogRepository;
import com.parklock.parklock_engine.repository.ParkingSpotRepository;
import com.parklock.parklock_engine.repository.ParkingTicketRepository;
import com.parklock.parklock_engine.strategy.PricingStrategyFactory;
import com.parklock.parklock_engine.vehicle.Vehicle;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ParkingServiceImpl implements ParkingService {

    private final ParkingSpotRepository parkingSpotRepository;
    private final ParkingTicketRepository parkingTicketRepository;
    private final PricingStrategyFactory pricingStrategyFactory;
    
    // Injected AuditLogRepository to handle tracking
    private final AuditLogRepository auditLogRepository;

    /*
     * INTERVIEW DEFENSE POINT (@Transactional + Distributed Pessimistic Lock):
     * We iterate through an unlocked snapshot of available spots and attempt to 
     * lock a single spot at the database level. This prevents race conditions 
     * while keeping the rest of the table unlocked for other concurrent requests.
     */
    @Override
    @Transactional
    public ParkingTicket parkVehicle(Vehicle vehicle) {
        // 1. Fetch available spots using an UNLOCKED snapshot read
        List<ParkingSpot> availableSpots = parkingSpotRepository.findByStatusAndSpotType(
                SpotStatus.AVAILABLE, 
                vehicle.getRequiredSpotType()
        );

        if (availableSpots.isEmpty()) {
            throw new IllegalStateException("No available parking spot found for type: " + vehicle.getRequiredSpotType());    
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

                // 5. Generate a unique ticket number and create a ParkingTicket
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
                
                // Save the ticket first so we have it fully initialized
                ParkingTicket savedTicket = parkingTicketRepository.save(ticket);

                // 6. Create the audit trail for parking
                AuditLog parkLog = new AuditLog(
                        AuditAction.VEHICLE_PARKED,
                        vehicle.getLicensePlate(),
                        savedTicket.getTicketNumber(),
                        lockedSpot.getId(),
                        "Vehicle entered and claimed spot ID: " + lockedSpot.getId()
                );
                auditLogRepository.save(parkLog);

                return savedTicket;
            }
        }

        // 7. If the loop finishes, heavy traffic grabbed all spots in our snapshot
        throw new IllegalStateException("Heavy traffic: All available spots were taken during booking. Please try again.");
    }

    @Override
    @Transactional
    public ParkingTicket unparkVehicle(String ticketNumber) {
        // 1. Retrieve the active ticket
        ParkingTicket ticket = parkingTicketRepository.findByTicketNumber(ticketNumber)
                .orElseThrow(() -> new IllegalStateException("Ticket not found: " + ticketNumber));

        if (ticket.getExitTime() != null) {
            throw new IllegalStateException("Vehicle has already exited with ticket: " + ticketNumber);
        }

        // 2. Set exit time and calculate duration
        LocalDateTime exitTime = LocalDateTime.now();
        ticket.setExitTime(exitTime);

        // Ensure a minimum of 1 hour is billed even if they leave immediately
        long durationInHours = Math.max(1, Duration.between(ticket.getEntryTime(), exitTime).toHours());

        // 3. Use Strategy Pattern Factory to calculate total fee
        double totalPrice = pricingStrategyFactory.getStrategy(ticket.getVehicleType()).calculatePrice(durationInHours);

        ticket.setTotalPrice(totalPrice);

        // 4. Free up the parking spot
        ParkingSpot spot = parkingSpotRepository.findById(ticket.getSpotId())
                .orElseThrow(() -> new IllegalStateException("Parking spot not found for ID: " + ticket.getSpotId()));

        spot.setStatus(SpotStatus.AVAILABLE);
        parkingSpotRepository.save(spot);
        
        // 5. Create the audit trail for unparking
        AuditLog unparkLog = new AuditLog(
                AuditAction.VEHICLE_UNPARKED,
                ticket.getLicensePlate(),
                ticket.getTicketNumber(),
                spot.getId(),
                "Vehicle exited. Total fee calculated: $" + totalPrice
        );
        auditLogRepository.save(unparkLog);

        return parkingTicketRepository.save(ticket);
    }
}