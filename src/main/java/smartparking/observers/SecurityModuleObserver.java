package smartparking.observers;

import smartparking.model.ParkingSpot;
import smartparking.model.SpotStatus;
import smartparking.observer.ParkingObserver;

/**
 * Observador que simula un módulo de seguridad.
 * Se interesa principalmente por plazas en avería.
 */
public class SecurityModuleObserver implements ParkingObserver {

    @Override
    public void update(ParkingSpot spot) {
        if (spot.getStatus() == SpotStatus.OUT_OF_SERVICE) {
            System.out.println("[SecurityModule] ALERTA: "
                    + "La plaza " + spot.getId()
                    + " ha pasado a estado OUT_OF_SERVICE. "
                    + "Enviar técnico o revisar incidente.");
        } else {
            System.out.println("[SecurityModule] Notificación: "
                    + "Cambio en plaza " + spot.getId()
                    + " -> " + spot.getStatus());
        }
    }
}


