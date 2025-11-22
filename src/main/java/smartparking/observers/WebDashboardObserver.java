package smartparking.observers;

import smartparking.model.ParkingLot;
import smartparking.model.ParkingSpot;
import smartparking.model.SpotStatus;
import smartparking.observer.ParkingObserver;

/**
 * Observador que simula un panel web en tiempo real.
 */
public class WebDashboardObserver implements ParkingObserver {

    private final ParkingLot parkingLot;

    public WebDashboardObserver(ParkingLot parkingLot) {
        this.parkingLot = parkingLot;
    }

    @Override
    public void update(ParkingSpot spot) {
        System.out.println("[WebDashboard] Actualización recibida: "
                + "Plaza " + spot.getId() + " -> " + spot.getStatus());

        long free = parkingLot.getSpots().stream()
                .filter(s -> s.getStatus() == SpotStatus.FREE)
                .count();
        long occupied = parkingLot.getSpots().stream()
                .filter(s -> s.getStatus() == SpotStatus.OCCUPIED)
                .count();
        long outOfService = parkingLot.getSpots().stream()
                .filter(s -> s.getStatus() == SpotStatus.OUT_OF_SERVICE)
                .count();

        System.out.println("[WebDashboard] Resumen actual: "
                + "Libres=" + free
                + ", Ocupadas=" + occupied
                + ", Avería=" + outOfService);
        System.out.println("--------------------------------------------------");
    }
}


