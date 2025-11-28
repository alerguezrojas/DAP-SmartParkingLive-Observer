package smartparking.pricing;

/**
 * Respuesta con el desglose de una cotizacion.
 */
public record PricingQuote(
        double baseAmount,
        double peakSurcharge,
        double electricSurcharge,
        double discount,
        double tax,
        double total,
        String currency,
        boolean peakApplied
) {
}
