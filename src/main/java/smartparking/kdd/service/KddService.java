package smartparking.kdd.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import smartparking.kdd.model.*;

import java.util.List;
import java.util.Optional;

@Service
public class KddService {

    private final KddUserRepository userRepository;
    private final KddEventRepository eventRepository;

    public KddService(KddUserRepository userRepository, KddEventRepository eventRepository) {
        this.userRepository = userRepository;
        this.eventRepository = eventRepository;
    }

    @Transactional
    public KddUser registerUser(String name, double lat, double lon, boolean isMod) {
        // Verificar si ya existe para no duplicar (opcional, por nombre)
        Optional<KddUser> existing = userRepository.findByName(name);
        if (existing.isPresent()) {
            // Actualizamos ubicación si ya existe
            // Nota: KddUser debería tener setters o crear uno nuevo manteniendo el ID
            return existing.get(); 
        }

        KddUser user = new KddUser(null, name, new Location(lat, lon), isMod);
        return userRepository.save(user);
    }

    @Transactional
    public KddEvent createEvent(String name, String description, double lat, double lon, String creatorId) {
        KddUser creator = userRepository.findById(creatorId)
                .orElseThrow(() -> new IllegalArgumentException("Usuario creador no encontrado"));

        if (!creator.isMod()) {
            throw new SecurityException("Solo los moderadores pueden crear eventos");
        }

        KddEvent event = new KddEvent(name, description, new Location(lat, lon), creator.getName());
        event = eventRepository.save(event);

        notifyObservers(event);
        return event;
    }

    public List<KddEvent> getAllEvents() {
        return eventRepository.findAll();
    }

    public List<KddUser> getAllUsers() {
        return userRepository.findAll();
    }

    public Optional<KddUser> getUserByName(String name) {
        return userRepository.findByName(name);
    }

    @Transactional
    public KddUser updateUserLocation(String userId, double lat, double lon) {
        KddUser user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado con ID: " + userId));
        
        user.setLocation(new Location(lat, lon));
        return userRepository.save(user);
    }

    @Transactional
    public KddEvent joinEvent(String eventId, String userId) {
        KddEvent event = eventRepository.findById(eventId)
                .orElseThrow(() -> new IllegalArgumentException("Evento no encontrado"));
        
        KddUser user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));

        event.addParticipant(user.getName());
        return eventRepository.save(event);
    }

    @Transactional
    public KddChatMessage addMessageToEvent(String eventId, String userId, String content) {
        KddEvent event = eventRepository.findById(eventId)
                .orElseThrow(() -> new IllegalArgumentException("Evento no encontrado"));

        KddUser user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));

        // Validar si el usuario es participante (opcional)
        if (!event.getParticipants().contains(user.getName())) {
             throw new SecurityException("Debes unirte al evento para comentar");
        }

        KddChatMessage message = new KddChatMessage(user.getName(), content);
        event.addMessage(message);
        
        eventRepository.save(event); // Al guardar el evento, se guardan los mensajes gracias a @ElementCollection
        return message;
    }

    private void notifyObservers(KddEvent event) {
        // Recuperamos todos los usuarios para notificarles
        // Nota: En un sistema real con miles de usuarios, esto se haría con una query geoespacial en BD
        List<KddUser> users = userRepository.findAll();
        for (KddUser user : users) {
            user.onNewEvent(event);
        }
    }
}
