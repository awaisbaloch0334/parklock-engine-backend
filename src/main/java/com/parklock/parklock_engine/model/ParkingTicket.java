package com.parklock.parklock_engine.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "parking_tickets")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ParkingTicket {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String ticketNumber; // Unique ticket UUID generated at gate entry

    @Column(nullable = false)
    private Long spotId; // References assigned ParkingSpot ID

    @Column(nullable = false)
    private String licensePlate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SpotType vehicleType;

    @Column(nullable = false)
    private LocalDateTime entryTime;

    private LocalDateTime exitTime; // Nullable until vehicle exits

    private Double totalPrice; // Calculated at checkout using Strategy Pattern
}