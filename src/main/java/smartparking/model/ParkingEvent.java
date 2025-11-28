package smartparking.model;

import java.time.Instant;

/**
 * Evento de dominio para registrar cambios de estado de plazas.
 */
public record ParkingEvent(
        int spotId,
        SpotStatus status,
        Instant occurredAt,
        String source,
        String message
) {
}
