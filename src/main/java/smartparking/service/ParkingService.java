package smartparking.service;

import org.springframework.stereotype.Service;
import smartparking.config.ParkingProperties;
import smartparking.integration.CarparkSnapshot;
import smartparking.model.ParkingLot;
import smartparking.model.ParkingSpot;
import smartparking.model.SpotStatus;

import java.util.List;
import java.util.Optional;

/**
 * Servicio que gestiona la lógica de negocio del parking.
 */
@Service
public class ParkingService {

    private final ParkingLot parkingLot;
    private final ParkingProperties properties;

    public ParkingService(ParkingProperties properties) {
        this.properties = properties;
        this.parkingLot = new ParkingLot("SmartParking Live - Data Feed", properties.getDisplaySpots());
    }

    public ParkingLot getParkingLot() {
        return parkingLot;
    }

    public List<ParkingSpot> getAllSpots() {
        return parkingLot.getSpots();
    }

    public Optional<ParkingSpot> getSpotById(int id) {
        return parkingLot.findSpotById(id);
    }

    public boolean changeSpotStatus(int id, SpotStatus newStatus) {
        Optional<ParkingSpot> spot = parkingLot.findSpotById(id);
        if (spot.isPresent()) {
            parkingLot.changeSpotStatus(id, newStatus);
            return true;
        }
        return false;
    }

    /**
     * Actualiza el estado local a partir de datos externos de ocupación.
     */
    public synchronized void applyExternalSnapshot(CarparkSnapshot snapshot) {
        if (snapshot == null) {
            return;
        }

        // Sumarizamos todos los tipos para la visualizacion general
        int totalLots = snapshot.types().stream().mapToInt(t -> t.totalLots()).sum();
        int availableLots = snapshot.types().stream().mapToInt(t -> t.availableLots()).sum();

        if (totalLots <= 0) {
            setAll(SpotStatus.OUT_OF_SERVICE);
            return;
        }

        int targetSize = totalLots;
        if (properties.getMaxSpots() > 0) {
            targetSize = Math.min(totalLots, properties.getMaxSpots());
        }
        if (properties.isMirrorFeedSize()) {
            parkingLot.resizeTo(targetSize);
        }

        List<ParkingSpot> spots = parkingLot.getSpots();
        int capacity = spots.size();

        int freeSlotsShown = Math.min(Math.max(availableLots, 0), capacity);

        for (int i = 0; i < capacity; i++) {
            SpotStatus target = i < freeSlotsShown ? SpotStatus.FREE : SpotStatus.OCCUPIED;
            spots.get(i).setStatus(target);
        }
    }

    public synchronized void markOutOfService() {
        setAll(SpotStatus.OUT_OF_SERVICE);
    }

    private void setAll(SpotStatus status) {
        parkingLot.getSpots().forEach(spot -> spot.setStatus(status));
    }

    public ParkingStatistics getStatistics() {
        List<ParkingSpot> spots = parkingLot.getSpots();
        long free = spots.stream()
                .filter(s -> s.getStatus() == SpotStatus.FREE)
                .count();
        long occupied = spots.stream()
                .filter(s -> s.getStatus() == SpotStatus.OCCUPIED)
                .count();
        long outOfService = spots.stream()
                .filter(s -> s.getStatus() == SpotStatus.OUT_OF_SERVICE)
                .count();

        return new ParkingStatistics(
                parkingLot.getName(),
                spots.size(),
                (int) free,
                (int) occupied,
                (int) outOfService
        );
    }

    public static class ParkingStatistics {
        private String name;
        private int total;
        private int free;
        private int occupied;
        private int outOfService;

        public ParkingStatistics(String name, int total, int free, int occupied, int outOfService) {
            this.name = name;
            this.total = total;
            this.free = free;
            this.occupied = occupied;
            this.outOfService = outOfService;
        }

        // Getters
        public String getName() { return name; }
        public int getTotal() { return total; }
        public int getFree() { return free; }
        public int getOccupied() { return occupied; }
        public int getOutOfService() { return outOfService; }
    }
}
