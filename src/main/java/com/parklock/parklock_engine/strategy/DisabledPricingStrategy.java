package com.parklock.parklock_engine.strategy;

import org.springframework.stereotype.Component;

@Component
public class DisabledPricingStrategy implements PricingStrategy {

    private static final double HOURLY_RATE = 2.5; // 50% discounted rate

    @Override
    public double calculatePrice(long durationInHours) {
        long billableHours = Math.max(1, durationInHours);
        return billableHours * HOURLY_RATE;
    }
}