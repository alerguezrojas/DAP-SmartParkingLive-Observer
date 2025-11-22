package smartparking.observers;

import smartparking.model.ParkingLot;
import smartparking.model.ParkingSpot;
import smartparking.model.SpotStatus;
import smartparking.observer.ParkingObserver;

/**
 * Observador que calcula estadísticas simples del estado del parking.
 */
public class StatisticsModuleObserver implements ParkingObserver {

    private final ParkingLot parkingLot;

    private int totalChanges = 0;

    public StatisticsModuleObserver(ParkingLot parkingLot) {
        this.parkingLot = parkingLot;
    }

    @Override
    public void update(ParkingSpot spot) {
        totalChanges++;

        long occupied = parkingLot.getSpots().stream()
                .filter(s -> s.getStatus() == SpotStatus.OCCUPIED)
                .count();

        System.out.println("[StatisticsModule] Cambio #" + totalChanges
                + ": Plaza " + spot.getId() + " ahora está " + spot.getStatus()
                + ". Ocupadas totales: " + occupied + "/" + parkingLot.getSpots().size());
    }
}


