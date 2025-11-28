package smartparking.pricing;

/**
 * Estrategia para calcular tarifas de parking.
 */
public interface PricingStrategy {
    PricingQuote calculate(PricingRequest request);
}
