package com.parklock.parklock_engine.repository;

import com.parklock.parklock_engine.config.SystemConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SystemConfigRepository extends JpaRepository<SystemConfig, Long> {
    // Allows us to fetch settings by key (e.g., finding "PRICE_PER_HOUR")
    Optional<SystemConfig> findByConfigKey(String configKey);
}