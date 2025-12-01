package smartparking.service;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import java.util.Map;
import java.util.HashMap;

@Service
public class ParkingHistoryService {

    private final ParkingService parkingService;
    private final RealTimeParkingUpdater realTimeParkingUpdater;
    private final Map<String, List<HistoryPoint>> historyMap = java.util.Collections.synchronizedMap(new HashMap<>());
    
    private static final String HISTORY_FILE = "parking-history.json";
    private final ObjectMapper objectMapper = new ObjectMapper();

    public ParkingHistoryService(ParkingService parkingService, @org.springframework.context.annotation.Lazy RealTimeParkingUpdater realTimeParkingUpdater) {
        this.parkingService = parkingService;
        this.realTimeParkingUpdater = realTimeParkingUpdater;
        objectMapper.registerModule(new JavaTimeModule());
    }

    @PostConstruct
    public void loadHistory() {
        File file = new File(HISTORY_FILE);
        if (file.exists()) {
            try {
                Map<String, List<HistoryPoint>> loaded = objectMapper.readValue(file, new TypeReference<Map<String, List<HistoryPoint>>>() {});
                historyMap.putAll(loaded);
                System.out.println("Historial cargado: " + loaded.size() + " parkings.");
            } catch (IOException e) {
                try {
                    List<HistoryPoint> oldList = objectMapper.readValue(file, new TypeReference<List<HistoryPoint>>() {});
                    historyMap.put("LEGACY", oldList);
                    System.out.println("Historial legado cargado.");
                } catch (IOException ex) {
                    System.err.println("No se pudo cargar el historial: " + ex.getMessage());
                }
            }
        }
    }

    @PreDestroy
    public void saveHistory() {
        try {
            synchronized (historyMap) {
                objectMapper.writeValue(new File(HISTORY_FILE), historyMap);
            }
            System.out.println("Historial guardado en disco.");
        } catch (IOException e) {
            System.err.println("Error guardando historial: " + e.getMessage());
        }
    }

    // Guardado periodico cada 5 minutos para evitar perdida de datos si la app crashea
    @Scheduled(fixedRate = 300000) 
    public void saveHistoryPeriodic() {
        saveHistory();
    }

    @Scheduled(fixedRate = 60000) // Cada minuto
    public void captureSnapshot() {
        var stats = parkingService.getStatistics();
        String currentId = realTimeParkingUpdater.getLastSnapshot()
                .map(s -> s.carparkNumber())
                .orElse("UNKNOWN");

        HistoryPoint point = new HistoryPoint(
                LocalDateTime.now(),
                stats.getFree(),
                stats.getOccupied()
        );
        
        synchronized (historyMap) {
            historyMap.computeIfAbsent(currentId, k -> new ArrayList<>()).add(point);
        }
    }

    public List<HistoryPoint> getHistory() {
        String currentId = realTimeParkingUpdater.getLastSnapshot()
                .map(s -> s.carparkNumber())
                .orElse("UNKNOWN");

        synchronized (historyMap) {
            return new ArrayList<>(historyMap.getOrDefault(currentId, Collections.emptyList()));
        }
    }

    public record HistoryPoint(
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
        LocalDateTime timestamp, 
        int free, 
        int occupied
    ) {}
}
