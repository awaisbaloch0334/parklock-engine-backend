package com.parklock.parklock_engine.repository;

import com.parklock.parklock_engine.model.ParkingSpot;
import com.parklock.parklock_engine.model.SpotStatus;
import com.parklock.parklock_engine.model.SpotType;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ParkingSpotRepository extends JpaRepository<ParkingSpot, Long> {

    // 1. Unlocked read to show the user available options
    List<ParkingSpot> findByStatusAndSpotType(SpotStatus status, SpotType type);

    /*
     * INTERVIEW DEFENSE POINT (Pessimistic Write Locking):
     * We ONLY lock the single row being booked.
     * This prevents two concurrent users who selected the same spot 
     * from double-booking it, without blocking the rest of the parking lot.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM ParkingSpot p WHERE p.id = :id")
    Optional<ParkingSpot> findByIdWithLock(@Param("id") Long id);

    Optional<ParkingSpot> findBySpotNumber(String spotNumber);
}