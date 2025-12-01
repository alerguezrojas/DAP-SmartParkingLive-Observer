package smartparking.kdd.service;

import org.springframework.stereotype.Service;
import smartparking.kdd.model.KddEvent;
import smartparking.kdd.model.KddUser;
import smartparking.kdd.model.Location;
import smartparking.kdd.observer.KddObserver;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;

@Service
public class KddService {

    private final List<KddUser> users = new CopyOnWriteArrayList<>();
    private final List<KddEvent> events = new CopyOnWriteArrayList<>();

    // In-memory storage for simplicity, matching the original Kdd.java simulation
    
    public KddUser registerUser(String name, double lat, double lon, boolean isMod) {
        KddUser user = new KddUser(
            java.util.UUID.randomUUID().toString(),
            name,
            new Location(lat, lon),
            isMod
        );
        users.add(user);
        return user;
    }

    public void removeUser(String userId) {
        users.removeIf(u -> u.getId().equals(userId));
    }

    public KddEvent createEvent(String name, String description, double lat, double lon, String creatorId) {
        Optional<KddUser> creator = users.stream().filter(u -> u.getId().equals(creatorId)).findFirst();
        
        if (creator.isEmpty()) {
            throw new IllegalArgumentException("Usuario no encontrado");
        }
        
        if (!creator.get().isMod()) {
            throw new SecurityException("Solo los moderadores pueden crear eventos");
        }

        String creatorName = creator.get().getName();

        KddEvent event = new KddEvent(name, description, new Location(lat, lon), creatorName);
        // Automatically add creator as participant
        event.addParticipant(creatorName);
        
        events.add(event);

        notifyObservers(event);
        return event;
    }

    private void notifyObservers(KddEvent event) {
        // The KddUser implements KddObserver and has the logic to check distance
        for (KddUser user : users) {
            user.onNewEvent(event);
        }
    }

    public List<KddEvent> getAllEvents() {
        return new ArrayList<>(events);
    }
    
    public List<KddUser> getAllUsers() {
        return new ArrayList<>(users);
    }
    
    public Optional<KddUser> getUserByName(String name) {
        return users.stream()
                .filter(u -> u.getName().equalsIgnoreCase(name))
                .findFirst();
    }
    
    public Optional<KddUser> getUser(String id) {
        return users.stream().filter(u -> u.getId().equals(id)).findFirst();
    }

    public KddEvent joinEvent(String eventId, String userId) {
        KddEvent event = events.stream()
                .filter(e -> e.getId().equals(eventId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Evento no encontrado"));

        KddUser user = users.stream()
                .filter(u -> u.getId().equals(userId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));

        event.addParticipant(user.getName());
        return event;
    }

    public smartparking.kdd.model.KddChatMessage addMessageToEvent(String eventId, String userId, String content) {
        KddEvent event = events.stream()
                .filter(e -> e.getId().equals(eventId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Evento no encontrado"));

        KddUser user = users.stream()
                .filter(u -> u.getId().equals(userId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));

        if (!event.getParticipants().contains(user.getName())) {
             throw new SecurityException("Debes unirte al evento para comentar");
        }

        smartparking.kdd.model.KddChatMessage message = new smartparking.kdd.model.KddChatMessage(user.getName(), content);
        event.addMessage(message);
        return message;
    }
}
