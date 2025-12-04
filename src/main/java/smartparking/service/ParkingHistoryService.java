package smartparking.service;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import smartparking.repository.ParkingHistoryRepository;
import smartparking.model.ParkingHistoryEntity;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ParkingHistoryService {

    private final ParkingService parkingService;
    private final RealTimeParkingUpdater realTimeParkingUpdater;
    private final ParkingHistoryRepository repository;

    public ParkingHistoryService(
            ParkingService parkingService,
            @org.springframework.context.annotation.Lazy RealTimeParkingUpdater realTimeParkingUpdater,
            ParkingHistoryRepository repository) {
        this.parkingService = parkingService;
        this.realTimeParkingUpdater = realTimeParkingUpdater;
        this.repository = repository;
    }

    // Ya no necesitamos loadHistory() ni saveHistory() con archivos JSON.
    // La base de datos persiste automáticamente.

    @Scheduled(fixedRate = 60000) // Cada minuto
    public void captureSnapshot() {
        var stats = parkingService.getStatistics();
        String currentId = realTimeParkingUpdater.getLastSnapshot()
                .map(s -> s.carparkNumber())
                .orElse("UNKNOWN");

        ParkingHistoryEntity entity = new ParkingHistoryEntity(
                currentId,
                LocalDateTime.now(),
                stats.getFree(),
                stats.getOccupied()
        );

        repository.save(entity);
    }

    public List<HistoryPoint> getHistory() {
        String currentId = realTimeParkingUpdater.getLastSnapshot()
                .map(s -> s.carparkNumber())
                .orElse("UNKNOWN");

        return repository.findByCarparkIdOrderByTimestampAsc(currentId).stream()
                .map(e -> new HistoryPoint(e.getTimestamp(), e.getFree(), e.getOccupied()))
                .collect(Collectors.toList());
    }

    public record HistoryPoint(
            java.time.LocalDateTime timestamp,
            int free,
            int occupied
    ) {}
}
