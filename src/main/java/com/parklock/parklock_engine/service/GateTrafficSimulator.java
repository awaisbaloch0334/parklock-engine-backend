package com.parklock.parklock_engine.service;

import com.parklock.parklock_engine.dto.RaceSimulationResult;
import com.parklock.parklock_engine.model.ParkingSpot;
import com.parklock.parklock_engine.model.SpotStatus;
import com.parklock.parklock_engine.model.SpotType;
import com.parklock.parklock_engine.repository.ParkingSpotRepository;
import com.parklock.parklock_engine.vehicle.EVCar;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

@Service
@RequiredArgsConstructor
public class GateTrafficSimulator {

    private final ParkingAllocationService parkingAllocationService;
    private final ParkingSpotRepository parkingSpotRepository;

    /*
     * INTERVIEW DEFENSE POINT (CountDownLatch + ExecutorService):
     * A fixed thread pool of 20 threads is created to represent Gates A, B, and C.
     * CountDownLatch(1) acts as a starting gun so all 20 threads execute allocation
     * at the exact same millisecond to intentionally trigger a database race condition.
     */
    public RaceSimulationResult simulateRaceCondition(int concurrentThreads) throws InterruptedException {
        // 1. Prepare garage: Ensure exactly ONE EV_CHARGING spot is AVAILABLE
        prepareSingleSpotForRace();

        ExecutorService executor = Executors.newFixedThreadPool(concurrentThreads);
        CountDownLatch startGun = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(concurrentThreads);

        AtomicInteger successfulAllocations = new AtomicInteger(0);
        AtomicInteger rejectedRaceConditions = new AtomicInteger(0);

        String[] gates = {"Gate A", "Gate B", "Gate C"};

        for (int i = 1; i <= concurrentThreads; i++) {
            final int carNumber = i;
            final String assignedGate = gates[i % gates.length];

            executor.submit(() -> {
                try {
                    // All threads pause here until startGun.countDown() is called
                    startGun.await();

                    // Fire allocation request for an EV car
                    parkingAllocationService.allocateSpotWithLock(
                            new EVCar("RACE-EV-" + carNumber),
                            assignedGate
                    );

                    successfulAllocations.incrementAndGet();
                } catch (Exception e) {
                    // Any failed allocation (lock timeout or no spots left) counts as a safely rejected race condition
                    rejectedRaceConditions.incrementAndGet();
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        // 2. Fire the starting gun! Release all threads simultaneously
        startGun.countDown();

        // 3. Wait for all threads to finish processing
        doneLatch.await();
        executor.shutdown();

        // 4. Validate ACID integrity
        String status = (successfulAllocations.get() == 1 && rejectedRaceConditions.get() == (concurrentThreads - 1))
                ? "SUCCESS: Zero double-booking occurred. ACID integrity verified."
                : "WARNING: Unexpected allocation count. Check locking configuration.";

        return new RaceSimulationResult(
                concurrentThreads,
                successfulAllocations.get(),
                rejectedRaceConditions.get(),
                status
        );
    }

    @Transactional
    protected void prepareSingleSpotForRace() {
        List<ParkingSpot> evSpots = parkingSpotRepository.findAll().stream()
                .filter(spot -> spot.getSpotType() == SpotType.EV_CHARGING)
                .toList();

        for (int i = 0; i < evSpots.size(); i++) {
            ParkingSpot spot = evSpots.get(i);
            // Leave only the very first EV spot AVAILABLE; mark all others OCCUPIED
            spot.setStatus(i == 0 ? SpotStatus.AVAILABLE : SpotStatus.OCCUPIED);
        }
        parkingSpotRepository.saveAll(evSpots);
    }
}