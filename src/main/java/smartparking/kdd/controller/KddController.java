package smartparking.kdd.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.*;
import smartparking.kdd.model.KddEvent;
import smartparking.kdd.model.KddUser;
import smartparking.kdd.service.KddService;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/kdd")
@CrossOrigin(origins = "*")
public class KddController {

    private final KddService kddService;
    private final SimpMessagingTemplate messagingTemplate;

    public KddController(KddService kddService, SimpMessagingTemplate messagingTemplate) {
        this.kddService = kddService;
        this.messagingTemplate = messagingTemplate;
    }

    @PostMapping("/users")
    public ResponseEntity<KddUser> registerUser(@RequestBody Map<String, Object> body) {
        String name = (String) body.get("name");
        double lat = Double.parseDouble(body.get("lat").toString());
        double lon = Double.parseDouble(body.get("lon").toString());
        boolean isMod = Boolean.parseBoolean(body.getOrDefault("isMod", "false").toString());

        KddUser user = kddService.registerUser(name, lat, lon, isMod);

        // Attach Observer Callback
        // When this user is notified, we push a message to WebSocket
        user.setNotificationCallback(event -> {
            Map<String, Object> notification = Map.of(
                "targetUserId", user.getId(),
                "message", "Nuevo evento cerca: " + event.getName(),
                "event", event
            );
            messagingTemplate.convertAndSend("/topic/kdd/notifications", notification);
        });

        return ResponseEntity.ok(user);
    }

    @GetMapping("/events")
    public List<KddEvent> getEvents() {
        return kddService.getAllEvents();
    }

    @PostMapping("/events")
    public ResponseEntity<?> createEvent(@RequestBody Map<String, Object> body) {
        try {
            String name = (String) body.get("name");
            String description = (String) body.get("description");
            double lat = Double.parseDouble(body.get("lat").toString());
            double lon = Double.parseDouble(body.get("lon").toString());
            String creatorId = (String) body.get("creatorId");

            KddEvent event = kddService.createEvent(name, description, lat, lon, creatorId);
            return ResponseEntity.ok(event);
        } catch (SecurityException e) {
            return ResponseEntity.status(403).body(Map.of("error", e.getMessage()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", "Error interno"));
        }
    }

    @GetMapping("/users")
    public List<KddUser> getAllUsers() {
        return kddService.getAllUsers();
    }

    @GetMapping("/users/search")
    public ResponseEntity<?> getUserByName(@RequestParam String name) {
        return kddService.getUserByName(name)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/events/{eventId}/join")
    public ResponseEntity<?> joinEvent(@PathVariable String eventId, @RequestBody Map<String, String> body) {
        try {
            String userId = body.get("userId");
            if (userId == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "userId es requerido"));
            }
            KddEvent event = kddService.joinEvent(eventId, userId);
            return ResponseEntity.ok(event);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", "Error interno"));
        }
    }

    @PostMapping("/events/{eventId}/messages")
    public ResponseEntity<?> postMessage(@PathVariable String eventId, @RequestBody Map<String, String> body) {
        try {
            String userId = body.get("userId");
            String content = body.get("content");
            if (userId == null || content == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "userId y content son requeridos"));
            }
            var message = kddService.addMessageToEvent(eventId, userId, content);

            // Broadcast to WebSocket
            Map<String, Object> wsMessage = Map.of(
                "eventId", eventId,
                "senderName", message.getSenderName(),
                "content", message.getContent(),
                "timestamp", message.getTimestamp().toString()
            );
            messagingTemplate.convertAndSend("/topic/kdd/events/" + eventId + "/messages", wsMessage);

            return ResponseEntity.ok(message);
        } catch (SecurityException e) {
            return ResponseEntity.status(403).body(Map.of("error", e.getMessage()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", "Error interno"));
        }
    }
}
