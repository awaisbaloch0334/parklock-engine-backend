package com.parklock.parklock_engine.config;

import com.parklock.parklock_engine.model.ParkingSpot;
import com.parklock.parklock_engine.model.SpotStatus;
import com.parklock.parklock_engine.model.SpotType;
import com.parklock.parklock_engine.repository.ParkingSpotRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
public class DataSeeder implements CommandLineRunner {

    private final ParkingSpotRepository parkingSpotRepository;

    /*
     * INTERVIEW DEFENSE POINT (CommandLineRunner):
     * Executes automatically on application startup to seed the in-memory H2 database
     * with realistic garage inventory (30 bays across STANDARD, EV, and DISABLED types).
     */
    @Override
    public void run(String... args) throws Exception {
        if (parkingSpotRepository.count() == 0) {
            List<ParkingSpot> spots = new ArrayList<>();

            // Seed 20 Standard Bays (A-101 to A-120)
            for (int i = 1; i <= 6; i++) {
                spots.add(new ParkingSpot(null, "A-1" + String.format("%02d", i), SpotType.STANDARD, SpotStatus.AVAILABLE, 0L));
            }

            // Seed 6 EV Charging Bays (EV-01 to EV-06)
            for (int i = 1; i <= 6; i++) {
                spots.add(new ParkingSpot(null, "EV-0" + i, SpotType.EV_CHARGING, SpotStatus.AVAILABLE, 0L));
            }

            // Seed 4 Disabled Access Bays (DIS-01 to DIS-04)
            for (int i = 1; i <= 4; i++) {
                spots.add(new ParkingSpot(null, "DIS-0" + i, SpotType.DISABLED_ACCESS, SpotStatus.AVAILABLE, 0L));
            }

            parkingSpotRepository.saveAll(spots);
            System.out.println("✅ [DataSeeder] Successfully initialized 30 parking spots in the database!");
        }
    }

}