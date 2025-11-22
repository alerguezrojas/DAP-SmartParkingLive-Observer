package smartparking.observers;

import smartparking.model.ParkingSpot;
import smartparking.model.SpotStatus;
import smartparking.observer.ParkingObserver;

/**
 * Observador que simula una notificación a un móvil.
 * Por simplicidad, solo "escucha" una plaza concreta que le interesa al usuario.
 */
public class MobileNotifierObserver implements ParkingObserver {

    private final int interestedSpotId;

    public MobileNotifierObserver(int interestedSpotId) {
        this.interestedSpotId = interestedSpotId;
    }

    @Override
    public void update(ParkingSpot spot) {
        if (spot.getId() == interestedSpotId && spot.getStatus() == SpotStatus.FREE) {
            System.out.println("[MobileNotifier] Notificación al usuario: "
                    + "¡La plaza " + spot.getId() + " se ha liberado!");
        } else if (spot.getId() == interestedSpotId) {
            System.out.println("[MobileNotifier] Plaza " + spot.getId()
                    + " ahora está " + spot.getStatus());
        }
    }
}


