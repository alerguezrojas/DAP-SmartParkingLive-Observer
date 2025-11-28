package smartparking.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import smartparking.integration.CarparkSnapshot;
import smartparking.model.ParkingSpot;
import smartparking.model.SpotStatus;
import smartparking.service.ParkingService;
import smartparking.service.RealTimeParkingUpdater;

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

    public ParkingController(ParkingService parkingService, RealTimeParkingUpdater realTimeParkingUpdater) {
        this.parkingService = parkingService;
        this.realTimeParkingUpdater = realTimeParkingUpdater;
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
                snapshot.<Map<String, Object>>map(s -> Map.of(
                        "carparkNumber", s.carparkNumber(),
                        "availableLots", s.availableLots(),
                        "totalLots", s.totalLots(),
                        "open", s.open(),
                        "updatedAt", DateTimeFormatter.ISO_INSTANT.format(s.updatedAt()),
                        "source", "data.gov.sg/transport/carpark-availability"
                )).orElseGet(() -> Map.of(
                        "message", "Sin datos en vivo todavia",
                        "source", "data.gov.sg/transport/carpark-availability"
                ))
        );
    }
}

