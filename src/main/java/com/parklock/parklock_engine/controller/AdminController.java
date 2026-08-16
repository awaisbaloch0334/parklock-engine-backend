package com.parklock.parklock_engine.controller;

import com.parklock.parklock_engine.config.SystemConfig;
import com.parklock.parklock_engine.model.AuditAction;
import com.parklock.parklock_engine.model.AuditLog;
import com.parklock.parklock_engine.model.ParkingSpot;
import com.parklock.parklock_engine.model.SpotStatus;
import com.parklock.parklock_engine.repository.AuditLogRepository;
import com.parklock.parklock_engine.repository.ParkingSpotRepository;
import com.parklock.parklock_engine.repository.SystemConfigRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/admin")
public class AdminController {

    private final AuditLogRepository auditLogRepository;
    private final SystemConfigRepository systemConfigRepository;
    private final ParkingSpotRepository parkingSpotRepository;

    public AdminController(AuditLogRepository auditLogRepository, 
                           SystemConfigRepository systemConfigRepository,
                           ParkingSpotRepository parkingSpotRepository) {
        this.auditLogRepository = auditLogRepository;
        this.systemConfigRepository = systemConfigRepository;
        this.parkingSpotRepository = parkingSpotRepository;
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
        
        // 1. Update the Spot (This works!)
        spot.setStatus(SpotStatus.AVAILABLE); 
        parkingSpotRepository.save(spot);

        // 2. Safely try to save the Audit Log
        try {
            AuditLog audit = new AuditLog();
            audit.setAction(AuditAction.ADMIN_OVERRIDE);
            audit.setUserEmail(adminIdentifier);
            audit.setSpotId(spotId);
            audit.setDetails("Admin forcefully cleared parking bay #" + spotId);
            auditLogRepository.save(audit);
        } catch (Exception e) {
            // If it crashes, log it to Render but DO NOT crash the frontend response!
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

        // 1. Update the Price (This works!)
        SystemConfig config = systemConfigRepository.findByConfigKey("PRICE_PER_HOUR")
            .orElse(new SystemConfig()); 
        
        config.setConfigKey("PRICE_PER_HOUR");
        config.setConfigValue(String.valueOf(newPrice));
        systemConfigRepository.save(config);

        // 2. Safely try to save the Audit Log
        try {
            AuditLog audit = new AuditLog();
            audit.setAction(AuditAction.CONFIG_CHANGE);
            audit.setUserEmail(adminIdentifier);
            audit.setDetails("Updated hourly parking rate to $" + newPrice);
            auditLogRepository.save(audit);
        } catch (Exception e) {
             // If it crashes, log it to Render but DO NOT crash the frontend response!
             System.err.println("--- FAILED TO SAVE AUDIT LOG (PRICE) ---");
             System.err.println("Error: " + e.getMessage());
        }

        return ResponseEntity.ok(Map.of("message", "Price per hour successfully updated to $" + newPrice));
    }
}