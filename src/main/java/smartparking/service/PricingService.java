package smartparking.service;

import org.springframework.stereotype.Service;
import smartparking.pricing.PricingQuote;
import smartparking.pricing.PricingRequest;
import smartparking.pricing.PricingStrategy;

/**
 * Fachada de tarifas basada en Strategy para facilitar extensiones.
 */
@Service
public class PricingService {

    private final PricingStrategy pricingStrategy;

    public PricingService(PricingStrategy pricingStrategy) {
        this.pricingStrategy = pricingStrategy;
    }

    public PricingQuote quote(PricingRequest request) {
        return pricingStrategy.calculate(request);
    }
}
