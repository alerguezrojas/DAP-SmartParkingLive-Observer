package smartparking.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import smartparking.pricing.PricingQuote;
import smartparking.pricing.PricingRequest;
import smartparking.service.PricingService;

import java.util.Map;

/**
 * API para cotizaciones de precio de parking.
 */
@RestController
@RequestMapping("/api/parking/pricing")
@CrossOrigin(origins = "*")
public class PricingController {

    private final PricingService pricingService;

    public PricingController(PricingService pricingService) {
        this.pricingService = pricingService;
    }

    @PostMapping("/quote")
    public ResponseEntity<?> quote(@RequestBody PricingRequest request) {
        if (request == null || request.minutes() <= 0) {
            return ResponseEntity.badRequest().body(Map.of("error", "Debe indicar minutos positivos"));
        }
        PricingQuote quote = pricingService.quote(request);
        return ResponseEntity.ok(quote);
    }
}
