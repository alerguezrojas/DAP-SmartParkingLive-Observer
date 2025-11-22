package smartparking.service;

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

    private ParkingLot parkingLot;

    public ParkingService() {
        this.parkingLot = new ParkingLot("SmartParking Live - ULL", 10);
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


