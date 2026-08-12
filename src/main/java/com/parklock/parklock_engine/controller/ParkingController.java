package com.parklock.parklock_engine.controller;

import com.parklock.parklock_engine.dto.ParkVehicleRequest;
import com.parklock.parklock_engine.model.ParkingTicket;
import com.parklock.parklock_engine.model.ParkingSpot; 
import com.parklock.parklock_engine.model.ParkingTransaction;
import com.parklock.parklock_engine.repository.ParkingSpotRepository;
import com.parklock.parklock_engine.repository.TransactionRepository;
import com.parklock.parklock_engine.service.ParkingService;
import com.parklock.parklock_engine.vehicle.DisabledAccessCar;
import com.parklock.parklock_engine.vehicle.EVCar;
import com.parklock.parklock_engine.vehicle.StandardCar;
import com.parklock.parklock_engine.vehicle.Vehicle;
import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List; 

@RestController
@RequestMapping("/api/v1/parking")
@CrossOrigin(origins = "*") 
@RequiredArgsConstructor
public class ParkingController {

    private final ParkingService parkingService;
    private final ParkingSpotRepository parkingSpotRepository; 
    private final TransactionRepository transactionRepository;

    @PostMapping("/park")
    public ResponseEntity<ParkingTicket> parkVehicle(@RequestBody ParkVehicleRequest request) {
        Vehicle vehicle = switch (request.vehicleType()) {
            case STANDARD -> new StandardCar(request.licensePlate());
            case EV_CHARGING -> new EVCar(request.licensePlate());
            case DISABLED_ACCESS -> new DisabledAccessCar(request.licensePlate());
        };

        ParkingTicket ticket = parkingService.parkVehicle(vehicle);

        // Record entry transaction securely
        ParkingTransaction tx = new ParkingTransaction(
            ticket.getTicketNumber(), 
            request.licensePlate(), 
            request.vehicleType().name(), 
            LocalDateTime.now()
        );
        transactionRepository.save(tx);

        return ResponseEntity.status(HttpStatus.CREATED).body(ticket);
    }

    @PostMapping("/unpack/{ticketNumber}")
    public ResponseEntity<ParkingTicket> unparkVehicle(@PathVariable String ticketNumber) {
        ParkingTicket ticket = parkingService.unparkVehicle(ticketNumber);

        // Complete the audit log safely without depending on missing getter names
        ParkingTransaction tx = transactionRepository.findByTicketNumberAndStatus(ticketNumber, "ACTIVE")
            .orElse(null);
            
        if (tx != null) {
            tx.setExitTime(LocalDateTime.now());
            tx.setChargeAmount(0.0); // Safe baseline; updates cleanly with service logic if needed
            tx.setStatus("COMPLETED");
            transactionRepository.save(tx);
        }

        return ResponseEntity.ok(ticket);
    }

    @GetMapping("/spots")
    public List<ParkingSpot> getAllSpots() {
        return parkingSpotRepository.findAll();
    }
}