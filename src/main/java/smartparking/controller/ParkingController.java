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
import smartparking.repository.CarparkRepository;
import smartparking.model.Carpark;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

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
    private final CarparkRepository carparkRepository;

    public ParkingController(
            ParkingService parkingService,
            RealTimeParkingUpdater realTimeParkingUpdater,
            ParkingActivityLog parkingActivityLog,
            MonitoringService monitoringService,
            PricingService pricingService,
            ParkingHistoryService parkingHistoryService,
            CarparkRepository carparkRepository) {
        this.parkingService = parkingService;
        this.realTimeParkingUpdater = realTimeParkingUpdater;
        this.parkingActivityLog = parkingActivityLog;
        this.monitoringService = monitoringService;
        this.pricingService = pricingService;
        this.parkingHistoryService = parkingHistoryService;
        this.carparkRepository = carparkRepository;
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
                        "newStatus", newStatus.toString()));
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
                            "types", s.types());
                }).orElseGet(() -> Map.of(
                        "message", "Sin datos en vivo todavia",
                        "source", "data.gov.sg/transport/carpark-availability")));
    }

    @GetMapping("/list")
    public List<Map<String, Object>> listAllCarparks() {
        // Filter by parkings present in the database
        Set<String> allowedIds = carparkRepository.findAll().stream()
                .map(Carpark::getCarparkNumber)
                .collect(Collectors.toSet());

        return realTimeParkingUpdater.getAllSnapshots().stream()
                .filter(s -> allowedIds.contains(s.carparkNumber()))
                .map(s -> {
                    Map<String, Object> map = new java.util.HashMap<>();
                    map.put("id", s.carparkNumber());
                    map.put("totalLots", s.types().stream().mapToInt(t -> t.totalLots()).sum());
                    map.put("availableLots", s.types().stream().mapToInt(t -> t.availableLots()).sum());
                    return map;
                })
                .collect(Collectors.toList());
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
            @RequestParam(defaultValue = "false") boolean electric) {
        return ResponseEntity.ok(pricingService.calculateQuote(minutes, subscriber, electric));
    }

    /**
     * Obtiene el historial de ocupación.
     */
    @GetMapping("/history")
    public ResponseEntity<List<ParkingHistoryService.HistoryPoint>> getHistory() {
        return ResponseEntity.ok(parkingHistoryService.getHistory());
    }

    /**
     * Devuelve datos para el mapa: ubicación (lat/lon) y estado actual.
     */
    @GetMapping("/map-data")
    public ResponseEntity<List<Map<String, Object>>> getMapData() {
        List<smartparking.integration.CarparkSnapshot> snapshots = realTimeParkingUpdater.getAllSnapshots();
        List<smartparking.integration.CarparkMetadata> metadataList = realTimeParkingUpdater.getAllMetadata();

        // Index metadata by carpark number for fast lookup
        Map<String, smartparking.integration.CarparkMetadata> metaMap = metadataList.stream()
                .collect(java.util.stream.Collectors.toMap(
                        m -> m.carparkNumber(),
                        m -> m,
                        (existing, replacement) -> existing // keep first if duplicates
                ));

        List<Map<String, Object>> result = snapshots.stream()
                .map(snapshot -> {
                    smartparking.integration.CarparkMetadata meta = metaMap.get(snapshot.carparkNumber());
                    if (meta == null || meta.xCoord() == null || meta.yCoord() == null) {
                        return null;
                    }

                    try {
                        double x = Double.parseDouble(meta.xCoord());
                        double y = Double.parseDouble(meta.yCoord());
                        smartparking.util.SVY21Converter.LatLon latLon = smartparking.util.SVY21Converter
                                .computeLatLon(y, x); // Note: SVY21 N=y, E=x

                        int total = snapshot.types().stream().mapToInt(t -> t.totalLots()).sum();
                        int available = snapshot.types().stream().mapToInt(t -> t.availableLots()).sum();

                        Map<String, Object> map = new java.util.HashMap<>();
                        map.put("id", snapshot.carparkNumber());
                        map.put("address", meta.address());
                        map.put("lat", latLon.lat);
                        map.put("lon", latLon.lon);
                        map.put("total", total);
                        map.put("available", available);
                        map.put("type", meta.carparkType());
                        return map;
                    } catch (NumberFormatException e) {
                        return null;
                    }
                })
                .filter(java.util.Objects::nonNull)
                .collect(java.util.stream.Collectors.toList());

        return ResponseEntity.ok(result);
    }
}
