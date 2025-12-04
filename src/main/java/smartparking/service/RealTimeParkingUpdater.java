package smartparking.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import smartparking.config.ParkingProperties;
import smartparking.integration.CarparkSnapshot;
import smartparking.integration.SingaporeCarparkClient;
import smartparking.repository.CarparkRepository;
import smartparking.model.Carpark;

import jakarta.annotation.PostConstruct;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Sincroniza el estado local con una fuente de datos en tiempo real.
 */
@Component
public class RealTimeParkingUpdater {

    private static final Logger log = LoggerFactory.getLogger(RealTimeParkingUpdater.class);

    private final SingaporeCarparkClient client;
    private final ParkingService parkingService;
    private final ParkingProperties properties;
    private final CarparkRepository carparkRepository;

    private volatile CarparkSnapshot lastSnapshot;
    private volatile java.util.List<CarparkSnapshot> allSnapshots = java.util.Collections.emptyList();
    private volatile java.util.List<smartparking.integration.CarparkMetadata> allMetadata = java.util.Collections.emptyList();
    private volatile String activeCarparkId;

    public RealTimeParkingUpdater(
            SingaporeCarparkClient client,
            ParkingService parkingService,
            ParkingProperties properties,
            CarparkRepository carparkRepository
    ) {
        this.client = client;
        this.parkingService = parkingService;
        this.properties = properties;
        this.carparkRepository = carparkRepository;
    }

    @PostConstruct
    public void bootstrap() {
        // Ejecutamos la carga inicial en un hilo separado para no bloquear el arranque de la aplicación
        java.util.concurrent.CompletableFuture.runAsync(() -> {
            try {
                long count = carparkRepository.count();
                if (count > 0) {
                    log.info("Database check: Found {} existing records. Skipping initial import.", count);
                    // Aún así cargamos los metadatos en memoria por si se necesitan
                    long startFetch = System.currentTimeMillis();
                    this.allMetadata = client.fetchMetadata();
                    log.info("Metadata refresh: Loaded {} records into memory in {} ms", allMetadata.size(), (System.currentTimeMillis() - startFetch));
                } else {
                    log.info("Database check: Empty. Starting initial data import...");
                    
                    long startFetch = System.currentTimeMillis();
                    this.allMetadata = client.fetchMetadata();
                    long fetchTime = System.currentTimeMillis() - startFetch;
                    log.info("API Fetch: Retrieved {} records in {} ms", allMetadata.size(), fetchTime);
                    
                    if (!allMetadata.isEmpty()) {
                        java.util.List<Carpark> entities = allMetadata.stream()
                            .map(m -> new Carpark(
                                m.carparkNumber(),
                                m.address(),
                                m.xCoord(),
                                m.yCoord(),
                                m.carparkType(),
                                m.typeOfParkingSystem(),
                                m.shortTermParking(),
                                m.freeParking(),
                                m.nightParking(),
                                m.carparkDecks(),
                                m.gantryHeight(),
                                m.carparkBasement()
                            ))
                            .collect(Collectors.toCollection(java.util.ArrayList::new));
                        
                        log.info("Database Import: Starting batch insert of {} records...", entities.size());
                        long startSave = System.currentTimeMillis();
                        carparkRepository.saveAll(entities);
                        long saveTime = System.currentTimeMillis() - startSave;
                        
                        log.info("Database Import: COMPLETED. Saved {} records in {} ms (Avg: {} ms/record)", 
                            entities.size(), 
                            saveTime, 
                            String.format("%.2f", (double)saveTime / entities.size()));
                    } else {
                        log.warn("API Fetch: No records found. Skipping database import.");
                    }
                }
            } catch (Exception e) {
                log.error("Bootstrap Error: Failed to initialize parking data", e);
            }
            
            // Una vez terminada la carga de metadatos (o si falló), iniciamos el feed en tiempo real
            refreshFromFeed();
        });
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

    public java.util.List<smartparking.integration.CarparkMetadata> getAllMetadata() {
        return allMetadata;
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
