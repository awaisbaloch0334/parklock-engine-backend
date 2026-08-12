package com.parklock.parklock_engine.dto;

public record RaceSimulationResult(
    int totalRequestsFired,
    int successfulAllocations,
    int rejectedRaceConditions,
    String status
) {}