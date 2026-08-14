package com.parklock.parklock_engine.controller;

import com.parklock.parklock_engine.model.AuditLog;
import com.parklock.parklock_engine.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin/audit-logs")
@RequiredArgsConstructor
@CrossOrigin(origins = "*") // Allows your Vercel frontend to fetch this data
public class AuditLogController {

    private final AuditLogRepository auditLogRepository;

    /*
     * INTERVIEW DEFENSE POINT (Admin API):
     * This endpoint fetches the entire history of the garage, 
     * ordered from newest to oldest so the dashboard always 
     * shows the most recent events at the top.
     */
    @GetMapping
    public ResponseEntity<List<AuditLog>> getGarageHistory() {
        List<AuditLog> logs = auditLogRepository.findAllByOrderByTimestampDesc();
        return ResponseEntity.ok(logs);
    }
}