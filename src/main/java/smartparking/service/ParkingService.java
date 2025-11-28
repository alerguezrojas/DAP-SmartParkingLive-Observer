package smartparking.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
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

    public ParkingService(@Value("${parking.display-spots:20}") int displaySpots) {
        this.parkingLot = new ParkingLot("SmartParking Live - Data Feed", displaySpots);
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
    public synchronized void applyExternalSnapshot(int availableLots, int totalLots, boolean open) {
        if (!open || totalLots <= 0) {
            setAll(SpotStatus.OUT_OF_SERVICE);
            return;
        }

        List<ParkingSpot> spots = parkingLot.getSpots();
        int capacity = spots.size();

        // Ajustar a proporcionalidad para no marcar todo libre cuando el parking tiene más plazas que las que mostramos
        double freeRatio = Math.max(0d, Math.min(1d, (double) availableLots / (double) totalLots));
        int freeSlotsShown = (int) Math.round(freeRatio * capacity);

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
