package com.parklock.parklock_engine.strategy;

public interface PricingStrategy {
/*
     * INTERVIEW DEFENSE POINT (Strategy Pattern):
     * Defines a common contract for pricing algorithms.
     * New pricing rules (e.g., VIP, Weekend Rates) can be added without modifying existing code (Open/Closed Principle).
     */
    double calculatePrice(long durationInHours);
}