package smartparking.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import smartparking.config.ParkingProperties;
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
    private final ParkingProperties properties;

    private volatile CarparkSnapshot lastSnapshot;
    private volatile java.util.List<CarparkSnapshot> allSnapshots = java.util.Collections.emptyList();
    private volatile String activeCarparkId;

    public RealTimeParkingUpdater(
            SingaporeCarparkClient client,
            ParkingService parkingService,
            ParkingProperties properties
    ) {
        this.client = client;
        this.parkingService = parkingService;
        this.properties = properties;
    }

    @PostConstruct
    public void bootstrap() {
        refreshFromFeed();
    }

    @Scheduled(fixedDelayString = "${parking.update-interval-ms:30000}")
    public void refreshFromFeed() {
        var snapshots = client.fetchAll();
        this.allSnapshots = snapshots;
        
        if (snapshots.isEmpty()) {
            log.warn("No hay datos en vivo, marcando todas las plazas como fuera de servicio");
            parkingService.markOutOfService();
            return;
        }

        // Por defecto usamos el primer parking o el configurado
        String target = activeCarparkId;
        if (target == null) target = properties.getCarparkNumber();

        String finalTarget = target;
        CarparkSnapshot selected = snapshots.stream()
                .filter(s -> finalTarget == null || finalTarget.isBlank() || s.carparkNumber().equalsIgnoreCase(finalTarget))
                .findFirst()
                .orElse(snapshots.get(0));

        this.lastSnapshot = selected;
        parkingService.applyExternalSnapshot(selected);
        
        log.info("Actualizado estado desde feed para parking {}: {} libres / {} total", 
                selected.carparkNumber(), 
                selected.types().stream().mapToInt(t -> t.availableLots()).sum(),
                selected.types().stream().mapToInt(t -> t.totalLots()).sum());
    }

    public Optional<CarparkSnapshot> getLastSnapshot() {
        return Optional.ofNullable(lastSnapshot);
    }

    public java.util.List<CarparkSnapshot> getAllSnapshots() {
        return allSnapshots;
    }

    public boolean setActiveCarpark(String carparkId) {
        Optional<CarparkSnapshot> found = allSnapshots.stream()
            .filter(s -> s.carparkNumber().equalsIgnoreCase(carparkId))
            .findFirst();
            
        if (found.isPresent()) {
            this.activeCarparkId = carparkId;
            this.lastSnapshot = found.get();
            parkingService.applyExternalSnapshot(found.get());
            log.info("Cambiado parking activo a: {}", carparkId);
            return true;
        }
        return false;
    }
}
