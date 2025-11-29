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

@Service
public class ParkingHistoryService {

    private final ParkingService parkingService;
    private final List<HistoryPoint> history = Collections.synchronizedList(new LinkedList<>());
    // Eliminamos el limite para guardar todo el historico
    // private static final int MAX_HISTORY_POINTS = 1440; 
    private static final String HISTORY_FILE = "parking-history.json";
    private final ObjectMapper objectMapper = new ObjectMapper();

    public ParkingHistoryService(ParkingService parkingService) {
        this.parkingService = parkingService;
        objectMapper.registerModule(new JavaTimeModule());
    }

    @PostConstruct
    public void loadHistory() {
        File file = new File(HISTORY_FILE);
        if (file.exists()) {
            try {
                List<HistoryPoint> loaded = objectMapper.readValue(file, new TypeReference<List<HistoryPoint>>() {});
                history.addAll(loaded);
                System.out.println("Historial cargado: " + loaded.size() + " puntos.");
            } catch (IOException e) {
                System.err.println("No se pudo cargar el historial: " + e.getMessage());
            }
        }
    }

    @PreDestroy
    public void saveHistory() {
        try {
            synchronized (history) {
                objectMapper.writeValue(new File(HISTORY_FILE), history);
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
        HistoryPoint point = new HistoryPoint(
                LocalDateTime.now(),
                stats.getFree(),
                stats.getOccupied()
        );
        
        synchronized (history) {
            history.add(point);
            // Ya no eliminamos puntos antiguos
        }
    }

    public List<HistoryPoint> getHistory() {
        synchronized (history) {
            return new ArrayList<>(history);
        }
    }

    public record HistoryPoint(
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
        LocalDateTime timestamp, 
        int free, 
        int occupied
    ) {}
}
