package com.parklock.parklock_engine.controller;

import com.parklock.parklock_engine.dto.RaceSimulationResult;
import com.parklock.parklock_engine.service.GateTrafficSimulator;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/simulate")
@CrossOrigin(origin = "*") //Allows React development server to call this endpoint
@RequiredArgsConstructor
public class SimulationController {

    private final GateTrafficSimulator gateTrafficSimulator;

    /*
     * INTERVIEW DEFENSE POINT (Concurrency Stress Test Endpoint):
     * Triggers the multi-threaded GateTrafficSimulator from an HTTP POST request.
     * Example: POST /api/v1/simulate/race?concurrentThreads=20
     */
    public ResponseEntity<RaceSimulationResult> runRaceConditionSimulation(
        @RequestParam(defaultValue = "20") int concurrentThreads
    ) throws InterruptedException {

        RaceSimulationResult result = gateTrafficSimulator.simulateRaceCondition(concurrentThreads);
        return ResponseEntity.ok(result);
    }
}