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

    @Column(name = "user_email")
    private String userEmail; // Tracks which admin performed this action via Clerk

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
    private String details;

    // Default constructor
    public AuditLog() {}

    // Full constructor with userEmail
    public AuditLog(AuditAction action, String userEmail, String licensePlate, String ticketNumber, Long spotId, String details) {
        this.action = action;
        this.userEmail = userEmail;
        this.licensePlate = licensePlate;
        this.ticketNumber = ticketNumber;
        this.spotId = spotId;
        this.details = details;
    }

    // Getters and Setters
    public Long getId() { return id; }
    public AuditAction getAction() { return action; }
    public void setAction(AuditAction action) { this.action = action; }
    public String getUserEmail() { return userEmail; }
    public void setUserEmail(String userEmail) { this.userEmail = userEmail; }
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