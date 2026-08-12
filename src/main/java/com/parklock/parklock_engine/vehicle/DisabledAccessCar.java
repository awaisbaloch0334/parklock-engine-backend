package com.parklock.parklock_engine.vehicle;

import com.parklock.parklock_engine.model.SpotType;

public class DisabledAccessCar extends Vehicle {

    public DisabledAccessCar(String licensePlate) {
        super(licensePlate);
    }

    @Override
    public SpotType getRequiredSpotType() {
        return SpotType.DISABLED_ACCESS;
    }
}