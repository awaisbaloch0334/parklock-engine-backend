package com.parklock.parklock_engine.dto;

import com.parklock.parklock_engine.model.SpotType;

/*
     * INTERVIEW DEFENSE POINT (@Transactional + Pessimistic Lock):
     * @Transactional ensures the database connection stays open and holds the row-level lock
     * acquired by findAvailableSpotsWithLock() until the entire method completes or rolls back.
     */
public record ParkVehicleRequest(
    String licensePlate,
    SpotType vehicleType

) {}   