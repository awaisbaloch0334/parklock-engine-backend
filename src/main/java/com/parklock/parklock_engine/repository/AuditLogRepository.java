package com.parklock.parklock_engine.repository;

import com.parklock.parklock_engine.model.AuditAction;
import com.parklock.parklock_engine.model.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {
    
    // Gets the entire history, newest events first
    List<AuditLog> findAllByOrderByTimestampDesc();

    // Allows the admin to search for a specific customer's history
    List<AuditLog> findByLicensePlateOrderByTimestampDesc(String licensePlate);

    // Allows the admin to track specific ticket disputes
    List<AuditLog> findByTicketNumberOrderByTimestampDesc(String ticketNumber);

    // Useful for filtering the dashboard by event type (e.g., show all manual overrides)
    List<AuditLog> findByActionOrderByTimestampDesc(AuditAction action);
}