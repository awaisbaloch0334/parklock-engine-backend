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

    // 1. Get all audit logs
    @GetMapping("/audit-logs")
    public ResponseEntity<List<AuditLog>> getAuditLogs() {
        List<AuditLog> logs = auditLogRepository.findAllByOrderByTimestampDesc();
        return ResponseEntity.ok(logs);
    }

    // 2. Force Unpark / Override a stuck parking bay
    @PostMapping("/force-unpark/{spotId}")
    public ResponseEntity<Map<String, String>> forceUnpark(@PathVariable Long spotId, @AuthenticationPrincipal Jwt jwt) {
        String adminIdentifier = jwt != null && jwt.getClaimAsString("email") != null 
            ? jwt.getClaimAsString("email") 
            : (jwt != null ? jwt.getSubject() : "admin@parklock.com");

        // ACTUALLY UNPARK THE SPOT IN THE DATABASE
        ParkingSpot spot = parkingSpotRepository.findById(spotId).orElse(null);
        if (spot == null) {
            return ResponseEntity.status(404).body(Map.of("error", "Parking spot not found"));
        }
        
        // Update the spot status using your exact enum
        spot.setStatus(SpotStatus.AVAILABLE); 
        parkingSpotRepository.save(spot);

        // Record Audit Log
        AuditLog audit = new AuditLog(
            AuditAction.ADMIN_OVERRIDE,
            adminIdentifier,
            null,
            null,
            spotId,
            "Admin forcefully cleared parking bay #" + spotId
        );
        auditLogRepository.save(audit);

        // Return a proper JSON object so React doesn't crash!
        return ResponseEntity.ok(Map.of("message", "Bay " + spotId + " forcefully unparked."));
    }

    // 3. Get Current Price Per Hour
    @GetMapping("/config/price")
    public ResponseEntity<Map<String, String>> getPricing() {
        String price = systemConfigRepository.findByConfigKey("PRICE_PER_HOUR")
            .map(SystemConfig::getConfigValue)
            .orElse("5.00");
            
        return ResponseEntity.ok(Map.of("pricePerHour", price));
    }

    // 4. Adjust Price Per Hour
    @PostMapping("/config/price")
    public ResponseEntity<Map<String, String>> updatePricing(@RequestBody Map<String, Double> payload, @AuthenticationPrincipal Jwt jwt) {
        String adminIdentifier = jwt != null && jwt.getClaimAsString("email") != null 
            ? jwt.getClaimAsString("email") 
            : (jwt != null ? jwt.getSubject() : "admin@parklock.com");
            
        Double newPrice = payload.get("pricePerHour");

        if (newPrice == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "Price per hour cannot be null"));
        }

        SystemConfig config = systemConfigRepository.findByConfigKey("PRICE_PER_HOUR")
            .orElse(new SystemConfig("PRICE_PER_HOUR", "5.00"));
        
        config.setConfigValue(String.valueOf(newPrice));
        systemConfigRepository.save(config);

        AuditLog audit = new AuditLog(
            AuditAction.CONFIG_CHANGE,
            adminIdentifier,
            null,
            null,
            null,
            "Updated hourly parking rate to $" + newPrice
        );
        auditLogRepository.save(audit);

        return ResponseEntity.ok(Map.of("message", "Price per hour successfully updated to $" + newPrice));
    }
}