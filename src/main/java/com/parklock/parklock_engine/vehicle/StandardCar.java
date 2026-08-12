package com.parklock.parklock_engine.vehicle;

import com.parklock.parklock_engine.model.SpotType;

public class StandardCar extends Vehicle {

    public StandardCar(String licensePlate) {
        super(licensePlate);
    }

    @Override
    public SpotType getRequiredSpotType() {
        return SpotType.STANDARD;
    }
}