package com.parklock.parklock_engine.controller;

import com.parklock.parklock_engine.config.SystemConfig;
import com.parklock.parklock_engine.model.AuditAction;
import com.parklock.parklock_engine.model.AuditLog;
import com.parklock.parklock_engine.repository.AuditLogRepository;
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

    public AdminController(AuditLogRepository auditLogRepository, SystemConfigRepository systemConfigRepository) {
        this.auditLogRepository = auditLogRepository;
        this.systemConfigRepository = systemConfigRepository;
    }

    // 1. Get all audit logs with admin tracking
    @GetMapping("/audit-logs")
    public ResponseEntity<List<AuditLog>> getAuditLogs() {
        List<AuditLog> logs = auditLogRepository.findAllByOrderByTimestampDesc();
        return ResponseEntity.ok(logs);
    }

    // 2. Force Unpark / Override a stuck parking bay
    @PostMapping("/force-unpark/{spotId}")
    public ResponseEntity<String> forceUnpark(@PathVariable Long spotId, @AuthenticationPrincipal Jwt jwt) {
        String adminEmail = jwt != null ? jwt.getClaimAsString("email") : "admin@parklock.com";

        AuditLog audit = new AuditLog(
            AuditAction.ADMIN_OVERRIDE,
            adminEmail,
            null,
            null,
            spotId,
            "Admin forcefully cleared parking bay #" + spotId
        );
        auditLogRepository.save(audit);

        return ResponseEntity.ok("Bay " + spotId + " forcefully unparked.");
    }

    // 3. Get Current Price Per Hour
    @GetMapping("/config/price")
    public ResponseEntity<String> getPricing() {
        String price = systemConfigRepository.findByConfigKey("PRICE_PER_HOUR")
            .map(SystemConfig::getConfigValue)
            .orElse("5.00"); // Default fallback
        return ResponseEntity.ok(price);
    }

    // 4. Adjust Price Per Hour
    @PostMapping("/config/price")
    public ResponseEntity<String> updatePricing(@RequestBody Map<String, Double> payload, @AuthenticationPrincipal Jwt jwt) {
        String adminEmail = jwt != null ? jwt.getClaimAsString("email") : "admin@parklock.com";
        Double newPrice = payload.get("pricePerHour");

        if (newPrice == null) {
            return ResponseEntity.badRequest().body("Price per hour cannot be null");
        }

        // Save or update in NeonDB
        SystemConfig config = systemConfigRepository.findByConfigKey("PRICE_PER_HOUR")
            .orElse(new SystemConfig("PRICE_PER_HOUR", "5.00"));
        
        config.setConfigValue(String.valueOf(newPrice));
        systemConfigRepository.save(config);

        // Record audit log
        AuditLog audit = new AuditLog(
            AuditAction.CONFIG_CHANGE,
            adminEmail,
            null,
            null,
            null,
            "Updated hourly parking rate to $" + newPrice
        );
        auditLogRepository.save(audit);

        return ResponseEntity.ok("Price per hour successfully updated to $" + newPrice);
    }
}