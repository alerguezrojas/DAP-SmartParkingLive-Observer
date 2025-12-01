package smartparking.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import smartparking.integration.CarparkSnapshot;
import smartparking.model.ParkingEvent;
import smartparking.model.ParkingSpot;
import smartparking.model.SpotStatus;
import smartparking.service.MonitoringService;
import smartparking.service.ParkingActivityLog;
import smartparking.service.ParkingHistoryService;
import smartparking.service.ParkingService;
import smartparking.service.PricingService;
import smartparking.service.RealTimeParkingUpdater;
import smartparking.pricing.PricingQuote;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Controlador REST para el sistema de parking.
 */
@RestController
@RequestMapping("/api/parking")
@CrossOrigin(origins = "*")
public class ParkingController {

    private final ParkingService parkingService;
    private final RealTimeParkingUpdater realTimeParkingUpdater;
    private final ParkingActivityLog parkingActivityLog;
    private final MonitoringService monitoringService;
    private final PricingService pricingService;
    private final ParkingHistoryService parkingHistoryService;

    public ParkingController(
            ParkingService parkingService,
            RealTimeParkingUpdater realTimeParkingUpdater,
            ParkingActivityLog parkingActivityLog,
            MonitoringService monitoringService,
            PricingService pricingService,
            ParkingHistoryService parkingHistoryService
    ) {
        this.parkingService = parkingService;
        this.realTimeParkingUpdater = realTimeParkingUpdater;
        this.parkingActivityLog = parkingActivityLog;
        this.monitoringService = monitoringService;
        this.pricingService = pricingService;
        this.parkingHistoryService = parkingHistoryService;
    }

    /**
     * Obtiene todas las plazas del parking.
     */
    @GetMapping("/spots")
    public ResponseEntity<List<ParkingSpot>> getAllSpots() {
        return ResponseEntity.ok(parkingService.getAllSpots());
    }

    /**
     * Obtiene una plaza especifica por ID.
     */
    @GetMapping("/spots/{id}")
    public ResponseEntity<ParkingSpot> getSpot(@PathVariable int id) {
        return parkingService.getSpotById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Cambia el estado de una plaza.
     */
    @PutMapping("/spots/{id}/status")
    public ResponseEntity<Map<String, String>> changeSpotStatus(
            @PathVariable int id,
            @RequestBody Map<String, String> body) {

        try {
            SpotStatus newStatus = SpotStatus.valueOf(body.get("status"));
            boolean success = parkingService.changeSpotStatus(id, newStatus);

            if (success) {
                return ResponseEntity.ok(Map.of(
                        "message", "Estado actualizado correctamente",
                        "spotId", String.valueOf(id),
                        "newStatus", newStatus.toString()
                ));
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Estado invalido"));
        }
    }

    /**
     * Obtiene las estadisticas del parking.
     */
    @GetMapping("/statistics")
    public ResponseEntity<ParkingService.ParkingStatistics> getStatistics() {
        return ResponseEntity.ok(parkingService.getStatistics());
    }

    /**
     * Devuelve el ultimo snapshot de la fuente en tiempo real.
     */
    @GetMapping("/feed")
    public ResponseEntity<Map<String, Object>> getFeedSnapshot() {
        Optional<CarparkSnapshot> snapshot = realTimeParkingUpdater.getLastSnapshot();
        return ResponseEntity.ok(
                snapshot.<Map<String, Object>>map(s -> {
                    int total = s.types().stream().mapToInt(t -> t.totalLots()).sum();
                    int available = s.types().stream().mapToInt(t -> t.availableLots()).sum();
                    return Map.of(
                        "carparkNumber", s.carparkNumber(),
                        "availableLots", available,
                        "totalLots", total,
                        "open", true,
                        "updatedAt", DateTimeFormatter.ISO_INSTANT.format(s.updatedAt()),
                        "source", "data.gov.sg/transport/carpark-availability",
                        "types", s.types()
                    );
                }).orElseGet(() -> Map.of(
                        "message", "Sin datos en vivo todavia",
                        "source", "data.gov.sg/transport/carpark-availability"
                ))
        );
    }

    @GetMapping("/list")
    public List<Map<String, Object>> listAllCarparks() {
        return realTimeParkingUpdater.getAllSnapshots().stream()
            .map(s -> {
                Map<String, Object> map = new java.util.HashMap<>();
                map.put("id", s.carparkNumber());
                map.put("totalLots", s.types().stream().mapToInt(t -> t.totalLots()).sum());
                map.put("availableLots", s.types().stream().mapToInt(t -> t.availableLots()).sum());
                return map;
            })
            .collect(java.util.stream.Collectors.toList());
    }

    @org.springframework.web.bind.annotation.PostMapping("/select/{id}")
    public ResponseEntity<?> selectCarpark(@PathVariable String id) {
        boolean success = realTimeParkingUpdater.setActiveCarpark(id);
        if (success) {
            return ResponseEntity.ok().build();
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Devuelve el log mas reciente de eventos del parking.
     */
    @GetMapping("/events")
    public ResponseEntity<List<ParkingEvent>> getRecentEvents(
            @RequestParam(value = "limit", defaultValue = "50") int limit) {
        int safeLimit = Math.max(1, Math.min(limit, 200));
        return ResponseEntity.ok(parkingActivityLog.getRecent(safeLimit));
    }

    /**
     * Snapshot de salud combinando feed y estado interno.
     */
    @GetMapping("/health")
    public ResponseEntity<MonitoringService.HealthSnapshot> getHealth() {
        return ResponseEntity.ok(monitoringService.getHealthSnapshot());
    }

    /**
     * Calcula una cotización de precio basada en la ocupación actual.
     */
    @GetMapping("/quote")
    public ResponseEntity<PricingQuote> getQuote(
            @RequestParam(defaultValue = "60") int minutes,
            @RequestParam(defaultValue = "false") boolean subscriber,
            @RequestParam(defaultValue = "false") boolean electric
    ) {
        return ResponseEntity.ok(pricingService.calculateQuote(minutes, subscriber, electric));
    }

    /**
     * Obtiene el historial de ocupación.
     */
    @GetMapping("/history")
    public ResponseEntity<List<ParkingHistoryService.HistoryPoint>> getHistory() {
        return ResponseEntity.ok(parkingHistoryService.getHistory());
    }
}
