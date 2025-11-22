package smartparking.observers;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import smartparking.model.ParkingSpot;
import smartparking.observer.ParkingObserver;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

/**
 * Observador que publica las actualizaciones del parking vía WebSocket.
 */
public class WebSocketObserver implements ParkingObserver {

    private final SimpMessagingTemplate messagingTemplate;

    public WebSocketObserver(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    @Override
    public void update(ParkingSpot spot) {
        Map<String, Object> message = new HashMap<>();
        message.put("spotId", spot.getId());
        message.put("status", spot.getStatus().toString());
        message.put("timestamp", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));

        // Publica el mensaje a todos los clientes conectados
        messagingTemplate.convertAndSend("/topic/parking-updates", message);
    }
}


