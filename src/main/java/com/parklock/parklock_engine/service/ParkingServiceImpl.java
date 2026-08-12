package com.parklock.parklock_engine.service;

import com.parklock.parklock_engine.model.ParkingSpot;
import com.parklock.parklock_engine.model.ParkingTicket;
import com.parklock.parklock_engine.model.SpotStatus;
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

    /*
     * INTERVIEW DEFENSE POINT (@Transactional + Pessimistic Lock):
     * @Transactional ensures the database connection stays open and holds the row-level lock
     * acquired by findAvailableSpotsWithLock() until the entire method completes or rolls back.
     */
    @Override
    @Transactional
    public ParkingTicket parkVehicle(Vehicle vehicle) {
        // 1. Query available spots with a SELECT ... FOR UPDATE lock
        List<ParkingSpot> availableSpots = parkingSpotRepository.findAvailableSpotsWithLock(SpotStatus.AVAILABLE, vehicle.getRequiredSpotType());

        if (availableSpots.isEmpty()) {
            throw new IllegalStateException("No available parking spot found for type:" + vehicle.getRequiredSpotType());    
        }

        // 2. Assign the first available spot and mark as OCCUPIED
        ParkingSpot spot = availableSpots.get(0);
        spot.setStatus(SpotStatus.OCCUPIED);
        parkingSpotRepository.save(spot);

        // 3. Generate a unique ticket number and create a ParkingTicket
        ParkingTicket ticket = new ParkingTicket(
        null,
        UUID.randomUUID().toString(),
        spot.getId(),
        vehicle.getLicensePlate(),
        vehicle.getRequiredSpotType(),
        LocalDateTime.now(),
        null,
        null
        );

        return parkingTicketRepository.save(ticket);
    }
    @Override
    @Transactional
    public ParkingTicket unparkVehicle(String ticketNumber) {
        // 1. Retrieve the active ticket
        ParkingTicket ticket = parkingTicketRepository.findByTicketNumber(ticketNumber).orElseThrow(() -> new IllegalStateException("Vehicle has already exited with ticket: " + ticketNumber));

        if (ticket.getExitTime() != null) {
            throw new IllegalStateException("Vehicle has already exited with ticket: " + ticketNumber);
        }

        // 2. Set exit time and calculate duration
        LocalDateTime exitTime = LocalDateTime.now();
        ticket.setExitTime(exitTime);

        long durationInHours = Duration.between(ticket.getEntryTime(), exitTime).toHours();

        // 3. Use Strategy Patrern Factory to calculate total fee
        double totalPrice = pricingStrategyFactory.getStrategy(ticket.getVehicleType()).calculatePrice(durationInHours);

        ticket.setTotalPrice(totalPrice);

        // 4. Free up the parking spot
        ParkingSpot spot = parkingSpotRepository.findById(ticket.getSpotId()).orElseThrow(() -> new IllegalStateException("Parking spot not found for ID:" + ticket.getSpotId()));

        spot.setStatus(SpotStatus.AVAILABLE);
        parkingSpotRepository.save(spot);

        return parkingTicketRepository.save(ticket);
    }

}

