package com.parklock.parklock_engine.model;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import java.time.LocalDateTime;

@Entity
@Table(name = "audit_log")
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AuditAction action;

    // These can be null, as some actions (like forcing a gate open) 
    // might not be associated with a specific vehicle or ticket.
    @Column(name = "license_plate")
    private String licensePlate;

    @Column(name = "ticket_number")
    private String ticketNumber;

    @Column(name = "spot_id")
    private Long spotId;

    @Column(nullable = false, updatable = false)
    @CreationTimestamp
    private LocalDateTime timestamp;

    @Column(length = 500)
    private String details; // A flexible text field for extra context

    // Default constructor required by JPA
    public AuditLog() {}

    // Constructor for easy logging
    public AuditLog(AuditAction action, String licensePlate, String ticketNumber, Long spotId, String details) {
        this.action = action;
        this.licensePlate = licensePlate;
        this.ticketNumber = ticketNumber;
        this.spotId = spotId;
        this.details = details;
    }

    // Getters and Setters
    public Long getId() { return id; }
    public AuditAction getAction() { return action; }
    public void setAction(AuditAction action) { this.action = action; }
    public String getLicensePlate() { return licensePlate; }
    public void setLicensePlate(String licensePlate) { this.licensePlate = licensePlate; }
    public String getTicketNumber() { return ticketNumber; }
    public void setTicketNumber(String ticketNumber) { this.ticketNumber = ticketNumber; }
    public Long getSpotId() { return spotId; }
    public void setSpotId(Long spotId) { this.spotId = spotId; }
    public LocalDateTime getTimestamp() { return timestamp; }
    public String getDetails() { return details; }
    public void setDetails(String details) { this.details = details; }
}