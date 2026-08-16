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
import java.util.Comparator; // Added import for sorting
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ParkingServiceImpl implements ParkingService {

    private final ParkingSpotRepository parkingSpotRepository;
    private final ParkingTicketRepository parkingTicketRepository;
    private final PricingStrategyFactory pricingStrategyFactory;
    private final AuditLogRepository auditLogRepository;

    @Override
    @Transactional
    public ParkingTicket parkVehicle(Vehicle vehicle) {
        List<ParkingSpot> availableSpots = parkingSpotRepository.findByStatusAndSpotType(
                SpotStatus.AVAILABLE, 
                vehicle.getRequiredSpotType()
        );

        if (availableSpots.isEmpty()) {
            throw new IllegalStateException("No available parking spot found for type: " + vehicle.getRequiredSpotType());    
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
                
                ParkingTicket savedTicket = parkingTicketRepository.save(ticket);

                AuditLog parkLog = new AuditLog(
                        AuditAction.VEHICLE_PARKED,
                        "system",
                        vehicle.getLicensePlate(),
                        savedTicket.getTicketNumber(),
                        lockedSpot.getId(),
                        "Vehicle entered and claimed spot ID: " + lockedSpot.getId()
                );
                auditLogRepository.save(parkLog);

                return savedTicket;
            }
        }

        throw new IllegalStateException("Heavy traffic: All available spots were taken during booking. Please try again.");
    }

    @Override
    @Transactional
    public ParkingTicket unparkVehicle(String ticketNumber) {
        ParkingTicket ticket = parkingTicketRepository.findByTicketNumber(ticketNumber)
                .orElseThrow(() -> new IllegalStateException("Ticket not found: " + ticketNumber));

        if (ticket.getExitTime() != null) {
            throw new IllegalStateException("Vehicle has already exited with ticket: " + ticketNumber);
        }

        LocalDateTime exitTime = LocalDateTime.now();
        ticket.setExitTime(exitTime);

        long durationInHours = Math.max(1, Duration.between(ticket.getEntryTime(), exitTime).toHours());
        double totalPrice = pricingStrategyFactory.getStrategy(ticket.getVehicleType()).calculatePrice(durationInHours);

        ticket.setTotalPrice(totalPrice);

        ParkingSpot spot = parkingSpotRepository.findById(ticket.getSpotId())
                .orElseThrow(() -> new IllegalStateException("Parking spot not found for ID: " + ticket.getSpotId()));

        spot.setStatus(SpotStatus.AVAILABLE);
        parkingSpotRepository.save(spot);
        
        AuditLog unparkLog = new AuditLog(
                AuditAction.VEHICLE_UNPARKED,
                "system",
                ticket.getLicensePlate(),
                ticket.getTicketNumber(),
                spot.getId(),
                "Vehicle exited. Total fee calculated: $" + totalPrice
        );
        auditLogRepository.save(unparkLog);

        return parkingTicketRepository.save(ticket);
    }
}