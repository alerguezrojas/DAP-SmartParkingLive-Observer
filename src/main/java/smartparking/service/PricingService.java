package smartparking.service;

import org.springframework.stereotype.Service;
import smartparking.pricing.PricingQuote;
import smartparking.pricing.PricingRequest;
import smartparking.pricing.PricingStrategy;

import java.time.LocalDateTime;

/**
 * Fachada de tarifas basada en Strategy para facilitar extensiones.
 */
@Service
public class PricingService {

    private final PricingStrategy pricingStrategy;
    private final ParkingService parkingService;

    public PricingService(PricingStrategy pricingStrategy, ParkingService parkingService) {
        this.pricingStrategy = pricingStrategy;
        this.parkingService = parkingService;
    }

    public PricingQuote quote(PricingRequest request) {
        return pricingStrategy.calculate(request);
    }

    public PricingQuote calculateQuote(int minutes, boolean subscriber, boolean electricVehicle) {
        var stats = parkingService.getStatistics();
        double occupancyRate = 0.0;
        if (stats.getTotal() > 0) {
            occupancyRate = (double) stats.getOccupied() / stats.getTotal();
        }

        PricingRequest request = new PricingRequest(
                minutes,
                subscriber,
                electricVehicle,
                LocalDateTime.now(),
                occupancyRate
        );
        return pricingStrategy.calculate(request);
    }
}
