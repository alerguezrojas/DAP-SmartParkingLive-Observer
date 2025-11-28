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
    private final List<ParkingObserver> registeredObservers = new ArrayList<>();

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
        if (observer == null) {
            return;
        }
        registeredObservers.add(observer);
        for (ParkingSpot spot : spots) {
            spot.attach(observer);
        }
    }

    /**
     * Ajusta el número de plazas al valor deseado, útil cuando el feed trae el total real.
     * Añade nuevas plazas con los observadores ya registrados; si se reduce, elimina las últimas.
     */
    public synchronized void resizeTo(int desiredSpots) {
        if (desiredSpots <= 0) {
            return;
        }

        int current = spots.size();
        if (desiredSpots > current) {
            for (int i = current + 1; i <= desiredSpots; i++) {
                ParkingSpot spot = new ParkingSpot(i);
                // Propagamos observadores ya registrados
                for (ParkingObserver observer : registeredObservers) {
                    spot.attach(observer);
                }
                spots.add(spot);
            }
        } else if (desiredSpots < current) {
            // Eliminamos plazas al final para reflejar fielmente el total informado
            spots.subList(desiredSpots, current).clear();
        }
    }
}

