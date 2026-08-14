package com.parklock.parklock_engine.controller;

import com.parklock.parklock_engine.dto.RaceSimulationResult;
import com.parklock.parklock_engine.service.GateTrafficSimulator;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/simulate")
@CrossOrigin(origins = "*") // FIX: Added the 's' to origins
@RequiredArgsConstructor
public class SimulationController {

    private final GateTrafficSimulator gateTrafficSimulator;

    /*
     * INTERVIEW DEFENSE POINT (Concurrency Stress Test Endpoint):
     * Triggers the multi-threaded GateTrafficSimulator from an HTTP POST request.
     * Example: POST /api/v1/simulate/race?concurrentThreads=20
     */
    @PostMapping("/race") // FIX: Added the HTTP mapping!
    public ResponseEntity<RaceSimulationResult> runRaceConditionSimulation(
        @RequestParam(defaultValue = "20") int concurrentThreads
    ) throws InterruptedException {

        RaceSimulationResult result = gateTrafficSimulator.simulateRaceCondition(concurrentThreads);
        return ResponseEntity.ok(result);
    }
}