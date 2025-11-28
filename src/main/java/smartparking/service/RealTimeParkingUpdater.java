package smartparking.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import smartparking.integration.CarparkSnapshot;
import smartparking.integration.SingaporeCarparkClient;

import jakarta.annotation.PostConstruct;
import java.util.Optional;

/**
 * Sincroniza el estado local con una fuente de datos en tiempo real.
 */
@Component
public class RealTimeParkingUpdater {

    private static final Logger log = LoggerFactory.getLogger(RealTimeParkingUpdater.class);

    private final SingaporeCarparkClient client;
    private final ParkingService parkingService;
    private final String carparkNumber;

    private volatile CarparkSnapshot lastSnapshot;

    public RealTimeParkingUpdater(
            SingaporeCarparkClient client,
            ParkingService parkingService,
            @Value("${parking.carpark-number:}") String carparkNumber
    ) {
        this.client = client;
        this.parkingService = parkingService;
        this.carparkNumber = carparkNumber;
    }

    @PostConstruct
    public void bootstrap() {
        refreshFromFeed();
    }

    @Scheduled(fixedDelayString = "${parking.update-interval-ms:30000}")
    public void refreshFromFeed() {
        Optional<CarparkSnapshot> snapshot = client.fetchSnapshot(carparkNumber);

        snapshot.ifPresentOrElse(
                this::applySnapshot,
                () -> {
                    log.warn("No hay datos en vivo, marcando todas las plazas como fuera de servicio");
                    parkingService.markOutOfService();
                }
        );
    }

    private void applySnapshot(CarparkSnapshot snapshot) {
        lastSnapshot = snapshot;
        parkingService.applyExternalSnapshot(
                snapshot.availableLots(),
                snapshot.totalLots(),
                snapshot.open()
        );
        log.debug("Feed {} => libres {}/{} (open: {}) at {}", snapshot.carparkNumber(),
                snapshot.availableLots(), snapshot.totalLots(), snapshot.open(), snapshot.updatedAt());
    }

    public Optional<CarparkSnapshot> getLastSnapshot() {
        return Optional.ofNullable(lastSnapshot);
    }
}
