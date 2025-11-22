package smartparking.model;

import smartparking.observer.ParkingObserver;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Representa un aparcamiento completo compuesto por varias plazas.
 */
public class ParkingLot {

    private final String name;
    private final List<ParkingSpot> spots;

    public ParkingLot(String name, int numberOfSpots) {
        this.name = name;
        this.spots = new ArrayList<>();
        for (int i = 1; i <= numberOfSpots; i++) {
            spots.add(new ParkingSpot(i));
        }
    }

    public String getName() {
        return name;
    }

    public List<ParkingSpot> getSpots() {
        return spots;
    }

    public Optional<ParkingSpot> findSpotById(int id) {
        return spots.stream()
                .filter(s -> s.getId() == id)
                .findFirst();
    }

    /**
     * Cambia el estado de una plaza concreta.
     */
    public void changeSpotStatus(int id, SpotStatus newStatus) {
        findSpotById(id).ifPresent(spot -> spot.setStatus(newStatus));
    }

    /**
     * Registra un observador en TODAS las plazas del parking.
     */
    public void attachObserverToAllSpots(ParkingObserver observer) {
        for (ParkingSpot spot : spots) {
            spot.attach(observer);
        }
    }
}

