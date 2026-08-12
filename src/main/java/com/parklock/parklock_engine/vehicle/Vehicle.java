package com.parklock.parklock_engine.vehicle;

import com.parklock.parklock_engine.model.SpotType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public abstract class Vehicle {

    private String licensePlate;

    // Polymorphic method: Each subclass determines its required spot type
    public abstract SpotType getRequiredSpotType();
}