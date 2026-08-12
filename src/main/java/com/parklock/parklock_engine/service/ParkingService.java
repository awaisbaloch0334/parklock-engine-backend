package com.parklock.parklock_engine.service;

import com.parklock.parklock_engine.model.ParkingTicket;
import com.parklock.parklock_engine.vehicle.Vehicle;

public interface ParkingService {

    /*
     * Assigns a parking spot to a vehicle using database-level pessimistic locking
     * and generates an entry ticket.
     */
    ParkingTicket parkVehicle(Vehicle vehicle);

    /*
     * Processes vehicle checkout, calculates dynamic fee using the Strategy Pattern,
     * and frees the parking spot.
     */
    ParkingTicket unparkVehicle(String ticketNumber);
}