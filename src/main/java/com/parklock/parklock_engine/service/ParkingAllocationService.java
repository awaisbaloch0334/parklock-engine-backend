package com.parklock.parklock_engine.service;

import com.parklock.parklock_engine.model.ParkingSpot;
import com.parklock.parklock_engine.model.ParkingTicket;
import com.parklock.parklock_engine.model.SpotStatus;
import com.parklock.parklock_engine.repository.ParkingSpotRepository;
import com.parklock.parklock_engine.repository.ParkingTicketRepository;
import com.parklock.parklock_engine.vehicle.Vehicle;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

@Service
@RequiredArgsConstructor
public class ParkingAllocationService {

    private final ParkingSpotRepository parkingSpotRepository;
    private final ParkingTicketRepository parkingTicketRepository;
    
    /*
     * INTERVIEW DEFENSE POINT (In-Memory Mutex Locking):
     * ConcurrentHashMap stores a dedicated ReentrantLock for each Spot ID.
     * This ensures that even if 20 threads from Gate A, B, and C hit the backend
     * at the exact same millisecond, only ONE thread can acquire the lock for a specific spot.
     */
    private final ConcurrentHashMap<Long, ReentrantLock> spotLocks = new ConcurrentHashMap<>();
    private ReentrantLock getLockForSpot(Long spotId) {
        return spotLocks.computeIfAbsent(spotId, id -> new ReentrantLock());
    }

    @Transactional
    public ParkingTicket allocateSpotWithLock(Vehicle vehicle, String gateName) {
        // 1. Fetch available spots using DB Pessimistic Lock
        List<ParkingSpot> availableSpots = parkingSpotRepository.findAvailableSpotsWithLock(SpotStatus.AVAILABLE, vehicle.getRequiredSpotType());

        if (availableSpots.isEmpty()) {
            throw new IllegalStateException("No available spot found for type:" + vehicle.getRequiredSpotType());
        }

        //2. Target the available spot
        ParkingSpot targetSpot = availableSpots.get(0);
        ReentrantLock lock = getLockForSpot(targetSpot.getId());

        try {
           /*
             * 3. Attempt to acquire the in-memory ReentrantLock with a 500ms timeout.
             * If another gate thread is currently allocating this bay, tryLock() returns false.
             */
            boolean isLocked = lock.tryLock(500, TimeUnit.MILLISECONDS);
            
            if (!isLocked) {
                throw new IllegalStateException("Race Condition Rejected: Spot " + targetSpot.getSpotNumber() + "is currently being locked by another gate thread!");
            }

            try {
                // Double-Check spot status after aquiring memory lock
                if (targetSpot.getStatus() != SpotStatus.AVAILABLE) {
                    throw new IllegalStateException("Spot " + targetSpot.getSpotNumber() + " was just occupied by another vehicle!");
                }

                // 4. Mark spot OCCUPIED and save
                targetSpot.setStatus(SpotStatus.OCCUPIED);
                parkingSpotRepository.save(targetSpot);

                //5. Generate and return entry ticket
                ParkingTicket ticket = new ParkingTicket(
                    null,
                    UUID.randomUUID().toString(),
                    targetSpot.getId(),
                    vehicle.getLicensePlate(),
                    vehicle.getRequiredSpotType(),
                    LocalDateTime.now(),
                    null,
                    null
                );

                return parkingTicketRepository.save(ticket);
            } finally {
                // Always unlock in a finally block to prevent deadlocks
                lock.unlock();
            }


        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Thread interrupted while waiting for spot lock on:" + targetSpot.getSpotNumber());
        }
    }
}