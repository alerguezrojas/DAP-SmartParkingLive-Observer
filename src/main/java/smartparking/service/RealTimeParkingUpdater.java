package smartparking.service;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import smartparking.config.ParkingProperties;
import smartparking.integration.CarparkMetadata;
import smartparking.integration.CarparkSnapshot;
import smartparking.integration.SingaporeCarparkClient;
import smartparking.model.Carpark;
import smartparking.repository.CarparkRepository;
import smartparking.repository.ParkingHistoryRepository;
import smartparking.model.ParkingHistoryEntity;
import smartparking.util.SVY21Converter;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
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
    private final ParkingHistoryRepository parkingHistoryRepository;

    private volatile CarparkSnapshot lastSnapshot;
    private volatile List<CarparkSnapshot> allSnapshots = Collections.emptyList();
    private volatile List<CarparkMetadata> allMetadata = Collections.emptyList();
    private volatile String activeCarparkId;
    
    // Cache for map data to avoid re-calculating on every request
    private volatile List<Map<String, Object>> cachedMapData = Collections.emptyList();

    private final Map<String, CarparkSnapshot> lastKnownStates = new ConcurrentHashMap<>();

    public RealTimeParkingUpdater(
            SingaporeCarparkClient client,
            ParkingService parkingService,
            ParkingProperties properties,
            CarparkRepository carparkRepository,
            ParkingHistoryRepository parkingHistoryRepository
    ) {
        this.client = client;
        this.parkingService = parkingService;
        this.properties = properties;
        this.carparkRepository = carparkRepository;
        this.parkingHistoryRepository = parkingHistoryRepository;
    }

    @PostConstruct
    public void bootstrap() {
        CompletableFuture.runAsync(this::performInitialLoad);
    }

    private void performInitialLoad() {
        try {
            long count = carparkRepository.count();
            if (count > 0) {
                log.info("Database check: Found {} existing records. Skipping initial import.", count);
                loadMetadataIntoMemory();
            } else {
                log.info("Database check: Empty. Starting initial data import...");
                importMetadataToDatabase();
            }
        } catch (Exception e) {
            log.error("Bootstrap Error: Failed to initialize parking data", e);
        }
        refreshFromFeed();
    }

    private void loadMetadataIntoMemory() {
        long startFetch = System.currentTimeMillis();
        this.allMetadata = client.fetchMetadata();
        log.info("Metadata refresh: Loaded {} records into memory in {} ms", allMetadata.size(), (System.currentTimeMillis() - startFetch));
    }

    private void importMetadataToDatabase() {
        long startFetch = System.currentTimeMillis();
        this.allMetadata = client.fetchMetadata();
        long fetchTime = System.currentTimeMillis() - startFetch;
        log.info("API Fetch: Retrieved {} records in {} ms", allMetadata.size(), fetchTime);

        if (!allMetadata.isEmpty()) {
            List<Carpark> entities = allMetadata.stream()
                    .map(this::mapToEntity)
                    .collect(Collectors.toList());

            log.info("Database Import: Starting batch insert of {} records...", entities.size());
            long startSave = System.currentTimeMillis();
            carparkRepository.saveAll(entities);
            long saveTime = System.currentTimeMillis() - startSave;

            log.info("Database Import: COMPLETED. Saved {} records in {} ms (Avg: {} ms/record)",
                    entities.size(), saveTime, String.format("%.2f", (double) saveTime / entities.size()));
        } else {
            log.warn("API Fetch: No records found. Skipping database import.");
        }
    }

    private Carpark mapToEntity(CarparkMetadata m) {
        return new Carpark(
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
        );
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

        updateHistory(snapshots);
        updateActiveCarpark(snapshots);
        updateMapDataCache(snapshots);
    }

    private void updateHistory(List<CarparkSnapshot> snapshots) {
        try {
            Set<String> knownIds = allMetadata.stream()
                    .map(CarparkMetadata::carparkNumber)
                    .collect(Collectors.toSet());

            List<ParkingHistoryEntity> historyToSave = new ArrayList<>();

            for (CarparkSnapshot snapshot : snapshots) {
                String id = snapshot.carparkNumber();
                if (knownIds.contains(id)) {
                    if (hasChanged(id, snapshot)) {
                        historyToSave.add(createHistoryEntity(snapshot));
                        lastKnownStates.put(id, snapshot);
                    }
                }
            }

            if (!historyToSave.isEmpty()) {
                parkingHistoryRepository.saveAll(historyToSave);
                log.info("History Update: Saved {} new records (changes detected)", historyToSave.size());
            }
        } catch (Exception e) {
            log.error("Error updating parking history", e);
        }
    }

    private boolean hasChanged(String id, CarparkSnapshot current) {
        CarparkSnapshot previous = lastKnownStates.get(id);
        if (previous == null) return true;

        int prevFree = previous.types().stream().mapToInt(t -> t.availableLots()).sum();
        int currFree = current.types().stream().mapToInt(t -> t.availableLots()).sum();
        return prevFree != currFree;
    }

    private ParkingHistoryEntity createHistoryEntity(CarparkSnapshot snapshot) {
        int total = snapshot.types().stream().mapToInt(t -> t.totalLots()).sum();
        int free = snapshot.types().stream().mapToInt(t -> t.availableLots()).sum();
        return new ParkingHistoryEntity(
                snapshot.carparkNumber(),
                LocalDateTime.ofInstant(snapshot.updatedAt(), ZoneId.systemDefault()),
                free,
                total - free
        );
    }

    private void updateActiveCarpark(List<CarparkSnapshot> snapshots) {
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
    
    private void updateMapDataCache(List<CarparkSnapshot> snapshots) {
         Map<String, CarparkMetadata> metaMap = allMetadata.stream()
                .collect(Collectors.toMap(
                        CarparkMetadata::carparkNumber,
                        m -> m,
                        (existing, replacement) -> existing
                ));

        this.cachedMapData = snapshots.stream()
                .map(snapshot -> buildMapData(snapshot, metaMap.get(snapshot.carparkNumber())))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }
    
    private Map<String, Object> buildMapData(CarparkSnapshot snapshot, CarparkMetadata meta) {
        if (meta == null || meta.xCoord() == null || meta.yCoord() == null) {
            return null;
        }
        try {
            double x = Double.parseDouble(meta.xCoord());
            double y = Double.parseDouble(meta.yCoord());
            SVY21Converter.LatLon latLon = SVY21Converter.computeLatLon(y, x);

            int total = snapshot.types().stream().mapToInt(t -> t.totalLots()).sum();
            int available = snapshot.types().stream().mapToInt(t -> t.availableLots()).sum();

            Map<String, Object> map = new HashMap<>();
            map.put("id", snapshot.carparkNumber());
            map.put("address", meta.address());
            map.put("lat", latLon.lat);
            map.put("lon", latLon.lon);
            map.put("total", total);
            map.put("available", available);
            map.put("type", meta.carparkType());
            return map;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    public Optional<CarparkSnapshot> getLastSnapshot() {
        return Optional.ofNullable(lastSnapshot);
    }

    public List<CarparkSnapshot> getAllSnapshots() {
        return allSnapshots;
    }

    public List<CarparkMetadata> getAllMetadata() {
        return allMetadata;
    }
    
    public List<Map<String, Object>> getCachedMapData() {
        return cachedMapData;
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
