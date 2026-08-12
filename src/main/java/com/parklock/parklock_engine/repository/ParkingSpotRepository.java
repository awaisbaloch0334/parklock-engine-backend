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

    /*
     * INTERVIEW DEFENSE POINT (Pessimistic Write Locking):
     * @Lock(LockModeType.PESSIMISTIC_WRITE) issues a "SELECT ... FOR UPDATE" SQL query.
     * This physically locks the matching database rows so no other concurrent transaction
     * can grab or modify the spot at the exact same millisecond.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM ParkingSpot p WHERE p.status = :status AND p.spotType = :type")
    List<ParkingSpot> findAvailableSpotsWithLock(@Param("status") SpotStatus status, @Param("type") SpotType type);

    Optional<ParkingSpot> findBySpotNumber(String spotNumber);
}