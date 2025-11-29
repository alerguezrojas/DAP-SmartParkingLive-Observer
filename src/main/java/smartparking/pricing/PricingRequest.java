package smartparking.pricing;

import java.time.LocalDateTime;

/**
 * Solicitud de cotizacion para una estancia de parking.
 */
public record PricingRequest(
        int minutes,
        boolean subscriber,
        boolean electricVehicle,
        LocalDateTime startTime,
        double occupancyRate
) {
}
