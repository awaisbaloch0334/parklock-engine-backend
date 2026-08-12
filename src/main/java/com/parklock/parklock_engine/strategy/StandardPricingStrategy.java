package com.parklock.parklock_engine.strategy;

import org.springframework.stereotype.Component;

@Component
public class StandardPricingStrategy implements PricingStrategy {

    private static final double HOURLY_RATE = 5.0; // $5 per Hour

    @Override
    public double calculatePrice(long durationInHours) {
        long billableHours = Math.max(1, durationInHours); // Minimum 1 hour charge
        return billableHours * HOURLY_RATE;
    }
}