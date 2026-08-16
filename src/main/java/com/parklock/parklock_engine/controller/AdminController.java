package com.parklock.parklock_engine.controller;

import com.parklock.parklock_engine.config.SystemConfig;
import com.parklock.parklock_engine.model.*;
import com.parklock.parklock_engine.repository.*;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/v1/admin")
public class AdminController {

    private final AuditLogRepository auditLogRepository;
    private final SystemConfigRepository systemConfigRepository;
    private final ParkingSpotRepository parkingSpotRepository;
    private final ParkingTicketRepository parkingTicketRepository; // INJECTED TICKET REPO
    private final TransactionRepository transactionRepository;     // INJECTED HISTORY REPO

    public AdminController(AuditLogRepository auditLogRepository, 
                           SystemConfigRepository systemConfigRepository,
                           ParkingSpotRepository parkingSpotRepository,
                           ParkingTicketRepository parkingTicketRepository,
                           TransactionRepository transactionRepository) {
        this.auditLogRepository = auditLogRepository;
        this.systemConfigRepository = systemConfigRepository;
        this.parkingSpotRepository = parkingSpotRepository;
        this.parkingTicketRepository = parkingTicketRepository;
        this.transactionRepository = transactionRepository;
    }

    @GetMapping("/audit-logs")
    public ResponseEntity<List<AuditLog>> getAuditLogs() {
        return ResponseEntity.ok(auditLogRepository.findAllByOrderByTimestampDesc());
    }

    @PostMapping("/force-unpark/{spotId}")
    public ResponseEntity<Map<String, String>> forceUnpark(@PathVariable Long spotId, @AuthenticationPrincipal Jwt jwt) {
        String adminIdentifier = jwt != null && jwt.getClaimAsString("email") != null 
            ? jwt.getClaimAsString("email") 
            : (jwt != null ? jwt.getSubject() : "admin@parklock.com");

        ParkingSpot spot = parkingSpotRepository.findById(spotId).orElse(null);
        if (spot == null) {
            return ResponseEntity.status(404).body(Map.of("error", "Parking spot not found"));
        }
        
        LocalDateTime now = LocalDateTime.now();
        String vehiclePlate = "UNKNOWN";

        // 1. FIND AND CLOSE THE ACTIVE PARKING TICKET
        Optional<ParkingTicket> activeTicketOpt = parkingTicketRepository.findBySpotIdAndExitTimeIsNull(spotId);
        
        if (activeTicketOpt.isPresent()) {
            ParkingTicket ticket = activeTicketOpt.get();
            ticket.setExitTime(now);
            ticket.setTotalPrice(0.0); // Override fee to $0 since Admin forced it
            vehiclePlate = ticket.getLicensePlate(); // Save plate for the audit log!
            parkingTicketRepository.save(ticket);

            // 2. FIND AND CLOSE THE PARKING HISTORY TRANSACTION
            Optional<ParkingTransaction> activeTransactionOpt = transactionRepository.findByTicketNumberAndStatus(ticket.getTicketNumber(), "ACTIVE");
            if (activeTransactionOpt.isPresent()) {
                ParkingTransaction transaction = activeTransactionOpt.get();
                transaction.setExitTime(now);
                transaction.setStatus("COMPLETED");
                transaction.setChargeAmount(0.0);
                transactionRepository.save(transaction);
            }
        }

        // 3. FREE UP THE PARKING SPOT
        spot.setStatus(SpotStatus.AVAILABLE); 
        parkingSpotRepository.save(spot);

        // 4. SAFELY SAVE THE AUDIT LOG
        try {
            AuditLog audit = new AuditLog();
            audit.setAction(AuditAction.ADMIN_OVERRIDE);
            audit.setUserEmail(adminIdentifier);
            audit.setSpotId(spotId);
            audit.setLicensePlate(vehiclePlate.equals("UNKNOWN") ? null : vehiclePlate);
            audit.setDetails("Admin forcefully cleared parking bay #" + spotId + " (Vehicle: " + vehiclePlate + ")");
            auditLogRepository.save(audit);
        } catch (Exception e) {
            System.err.println("--- FAILED TO SAVE AUDIT LOG (UNPARK) ---");
            System.err.println("Error: " + e.getMessage());
        }

        return ResponseEntity.ok(Map.of("message", "Bay " + spotId + " forcefully unparked."));
    }

    @GetMapping("/config/price")
    public ResponseEntity<String> getPricing() {
        String price = systemConfigRepository.findByConfigKey("PRICE_PER_HOUR")
            .map(SystemConfig::getConfigValue)
            .orElse("5.00");
            
        return ResponseEntity.ok(price);
    }

    @PostMapping("/config/price")
    public ResponseEntity<Map<String, String>> updatePricing(@RequestBody Map<String, Double> payload, @AuthenticationPrincipal Jwt jwt) {
        String adminIdentifier = jwt != null && jwt.getClaimAsString("email") != null 
            ? jwt.getClaimAsString("email") 
            : (jwt != null ? jwt.getSubject() : "admin@parklock.com");
            
        Double newPrice = payload.get("pricePerHour");

        if (newPrice == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "Price cannot be null"));
        }

        SystemConfig config = systemConfigRepository.findByConfigKey("PRICE_PER_HOUR")
            .orElse(new SystemConfig()); 
        
        config.setConfigKey("PRICE_PER_HOUR");
        config.setConfigValue(String.valueOf(newPrice));
        systemConfigRepository.save(config);

        try {
            AuditLog audit = new AuditLog();
            audit.setAction(AuditAction.CONFIG_CHANGE);
            audit.setUserEmail(adminIdentifier);
            audit.setDetails("Updated hourly parking rate to $" + newPrice);
            auditLogRepository.save(audit);
        } catch (Exception e) {
             System.err.println("--- FAILED TO SAVE AUDIT LOG (PRICE) ---");
             System.err.println("Error: " + e.getMessage());
        }

        return ResponseEntity.ok(Map.of("message", "Price per hour successfully updated to $" + newPrice));
    }
}