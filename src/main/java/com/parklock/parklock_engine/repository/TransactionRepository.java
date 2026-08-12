package com.parklock.parklock_engine.repository; // Adjust to your package name

import com.parklock.parklock_engine.model.ParkingTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface TransactionRepository extends JpaRepository<ParkingTransaction, Long> {
    Optional<ParkingTransaction> findByTicketNumberAndStatus(String ticketNumber, String status);
}