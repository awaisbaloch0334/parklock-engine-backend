package com.parklock.parklock_engine.model; // Adjust to your package name

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "parking_history")
public class ParkingTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String ticketNumber;
    private String licensePlate;
    private String vehicleType;
    
    private LocalDateTime entryTime;
    private LocalDateTime exitTime;
    
    private Double chargeAmount;
    private String status; // e.g., "ACTIVE", "COMPLETED"

    // Default Constructor
    public ParkingTransaction() {}

    // Constructor for entry
    public ParkingTransaction(String ticketNumber, String licensePlate, String vehicleType, LocalDateTime entryTime) {
        this.ticketNumber = ticketNumber;
        this.licensePlate = licensePlate;
        this.vehicleType = vehicleType;
        this.entryTime = entryTime;
        this.status = "ACTIVE";
    }

    // Getters and Setters
    public Long getId() { return id; }
    public String getTicketNumber() { return ticketNumber; }
    public void setTicketNumber(String ticketNumber) { this.ticketNumber = ticketNumber; }
    public String getLicensePlate() { return licensePlate; }
    public void setLicensePlate(String licensePlate) { this.licensePlate = licensePlate; }
    public String getVehicleType() { return vehicleType; }
    public void setVehicleType(String vehicleType) { this.vehicleType = vehicleType; }
    public LocalDateTime getEntryTime() { return entryTime; }
    public void setEntryTime(LocalDateTime entryTime) { this.entryTime = entryTime; }
    public LocalDateTime getExitTime() { return exitTime; }
    public void setExitTime(LocalDateTime exitTime) { this.exitTime = exitTime; }
    public Double getChargeAmount() { return chargeAmount; }
    public void setChargeAmount(Double chargeAmount) { this.chargeAmount = chargeAmount; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}