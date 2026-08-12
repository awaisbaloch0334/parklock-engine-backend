package com.parklock.parklock_engine.strategy;

import com.parklock.parklock_engine.model.SpotType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PricingStrategyFactory {

    private final StandardPricingStrategy standardPricingStrategy;
    private final EVPricingStrategy evPricingStrategy;
    private final DisabledPricingStrategy disabledPricingStrategy;
    /*
     * INTERVIEW DEFENSE POINT (Factory Pattern):
     * Encapsulates object selection logic so callers don't need to know which implementation class is used.
     */
    public PricingStrategy getStrategy(SpotType spotType) {
        return switch (spotType) {
            case STANDARD -> standardPricingStrategy;
            case EV_CHARGING -> evPricingStrategy;
            case DISABLED_ACCESS -> disabledPricingStrategy;
        };
    }
}