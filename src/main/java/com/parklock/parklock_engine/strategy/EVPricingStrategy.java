package com.parklock.parklock_engine.strategy;

import org.springframework.stereotype.Component;

@Component
public class EVPricingStrategy implements PricingStrategy {

    private static final double HOURLY_RATE = 5.0;
    private static final double EV_CHARGING_FEE_PER_HOUR = 3.0; // Extra charging fee

    @Override
    public double calculatePrice(long durationInHours) {
        long billableHours = Math.max(1, durationInHours);
        return billableHours * (HOURLY_RATE + EV_CHARGING_FEE_PER_HOUR);
    }
}