package smartparking.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import smartparking.integration.CarparkSnapshot;

import java.time.Duration;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

/**
 * Fachada de monitorizacion para ofrecer un snapshot de salud del sistema.
 */
@Service
public class MonitoringService {

    private final ParkingService parkingService;
    private final RealTimeParkingUpdater realTimeParkingUpdater;
    private final ParkingActivityLog activityLog;
    private final long maxFeedAgeMs;

    public MonitoringService(
            ParkingService parkingService,
            RealTimeParkingUpdater realTimeParkingUpdater,
            ParkingActivityLog activityLog,
            @Value("${parking.health.max-feed-age-ms:120000}") long maxFeedAgeMs
    ) {
        this.parkingService = parkingService;
        this.realTimeParkingUpdater = realTimeParkingUpdater;
        this.activityLog = activityLog;
        this.maxFeedAgeMs = maxFeedAgeMs;
    }

    public HealthSnapshot getHealthSnapshot() {
        ParkingService.ParkingStatistics statistics = parkingService.getStatistics();
        Optional<CarparkSnapshot> snapshot = realTimeParkingUpdater.getLastSnapshot();

        Long feedAgeMs = snapshot.map(s -> Duration.between(s.updatedAt(), Instant.now()).toMillis()).orElse(null);
        String lastFeedAt = snapshot.map(s -> DateTimeFormatter.ISO_INSTANT.format(s.updatedAt())).orElse(null);
        int recentEvents = (int) activityLog.countSince(Instant.now().minus(Duration.ofMinutes(10)));

        String status;
        String message;
        if (feedAgeMs == null) {
            status = "DEGRADED";
            message = "No hay datos recientes del feed";
        } else if (feedAgeMs <= maxFeedAgeMs) {
            status = "UP";
            message = "Feed en vivo disponible";
        } else {
            status = "DEGRADED";
            message = "Feed retrasado";
        }

        return new HealthSnapshot(
                status,
                statistics,
                lastFeedAt,
                feedAgeMs,
                recentEvents,
                message
        );
    }

    public record HealthSnapshot(
            String status,
            ParkingService.ParkingStatistics statistics,
            String lastFeedAt,
            Long feedAgeMs,
            int recentEvents,
            String message
    ) {
    }
}
