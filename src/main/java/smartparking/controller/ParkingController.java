package smartparking.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import smartparking.model.ParkingSpot;
import smartparking.model.SpotStatus;
import smartparking.service.ParkingService;

import java.util.List;
import java.util.Map;

/**
 * Controlador REST para el sistema de parking.
 */
@RestController
@RequestMapping("/api/parking")
@CrossOrigin(origins = "*")
public class ParkingController {

    @Autowired
    private ParkingService parkingService;

    /**
     * Obtiene todas las plazas del parking.
     */
    @GetMapping("/spots")
    public ResponseEntity<List<ParkingSpot>> getAllSpots() {
        return ResponseEntity.ok(parkingService.getAllSpots());
    }

    /**
     * Obtiene una plaza específica por ID.
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
                    .body(Map.of("error", "Estado inválido"));
        }
    }

    /**
     * Obtiene las estadísticas del parking.
     */
    @GetMapping("/statistics")
    public ResponseEntity<ParkingService.ParkingStatistics> getStatistics() {
        return ResponseEntity.ok(parkingService.getStatistics());
    }
}

