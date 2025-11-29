package smartparking.pricing;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Estrategia basica de precios con horas pico, recargo EV y descuento a suscriptores.
 */
@Component
public class DynamicPricingStrategy implements PricingStrategy {

    private final double baseRatePerHour;
    private final double peakMultiplier;
    private final double electricSurcharge;
    private final double subscriptionDiscount;
    private final double taxRate;
    private final String currency;
    private final List<PeakRange> peakRanges;

    public DynamicPricingStrategy(
            @Value("${pricing.base-rate-per-hour:2.5}") double baseRatePerHour,
            @Value("${pricing.peak-multiplier:1.35}") double peakMultiplier,
            @Value("${pricing.electric-surcharge:0.5}") double electricSurcharge,
            @Value("${pricing.subscription-discount:0.1}") double subscriptionDiscount,
            @Value("${pricing.tax-rate:0.21}") double taxRate,
            @Value("${pricing.currency:EUR}") String currency,
            @Value("${pricing.peak-ranges:07:00-10:00,17:00-20:00}") String peakRanges
    ) {
        this.baseRatePerHour = baseRatePerHour;
        this.peakMultiplier = peakMultiplier;
        this.electricSurcharge = electricSurcharge;
        this.subscriptionDiscount = subscriptionDiscount;
        this.taxRate = taxRate;
        this.currency = currency;
        this.peakRanges = parseRanges(peakRanges);
    }

    @Override
    public PricingQuote calculate(PricingRequest request) {
        int effectiveMinutes = Math.max(request.minutes(), 15);
        LocalDateTime startTime = request.startTime() != null ? request.startTime() : LocalDateTime.now();

        double hours = effectiveMinutes / 60.0;
        double baseAmount = hours * baseRatePerHour;

        boolean peak = isPeak(startTime.toLocalTime(), request.occupancyRate());
        double peakSurcharge = peak ? baseAmount * (peakMultiplier - 1.0) : 0;
        double evSurcharge = request.electricVehicle() ? electricSurcharge : 0;

        double subtotal = baseAmount + peakSurcharge + evSurcharge;
        double discount = request.subscriber() ? subtotal * subscriptionDiscount : 0;

        double taxable = subtotal - discount;
        double tax = taxable * taxRate;
        double total = taxable + tax;

        return new PricingQuote(
                round(baseAmount),
                round(peakSurcharge),
                round(evSurcharge),
                round(discount),
                round(tax),
                round(total),
                currency,
                peak
        );
    }

    private boolean isPeak(LocalTime time, double occupancyRate) {
        if (occupancyRate > 0.80) {
            return true;
        }
        for (PeakRange range : peakRanges) {
            if (range.includes(time)) {
                return true;
            }
        }
        return false;
    }

    private List<PeakRange> parseRanges(String ranges) {
        List<PeakRange> result = new ArrayList<>();
        if (ranges == null || ranges.isBlank()) {
            return result;
        }
        String[] pieces = ranges.split(",");
        for (String piece : pieces) {
            String[] parts = piece.split("-");
            if (parts.length == 2) {
                try {
                    LocalTime start = LocalTime.parse(parts[0]);
                    LocalTime end = LocalTime.parse(parts[1]);
                    result.add(new PeakRange(start, end));
                } catch (Exception ignored) {
                }
            }
        }
        return result;
    }

    private double round(double value) {
        return Math.round(value * 100.0) / 100.0;
    }

    private record PeakRange(LocalTime start, LocalTime end) {
        boolean includes(LocalTime time) {
            return !time.isBefore(start) && !time.isAfter(end);
        }
    }
}
