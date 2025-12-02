package smartparking.kdd.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "kdd_events")
public class KddEvent {
    
    @Id
    private String id;
    private String name;
    private String description;
    
    @Embedded
    private Location location;
    
    private String creatorName;
    private LocalDateTime createdAt;

    @ElementCollection
    @CollectionTable(name = "kdd_event_participants", joinColumns = @JoinColumn(name = "event_id"))
    @Column(name = "participant_name")
    private List<String> participants;

    @ElementCollection
    @CollectionTable(name = "kdd_event_messages", joinColumns = @JoinColumn(name = "event_id"))
    private List<KddChatMessage> chatMessages;

    public KddEvent() {}

    public KddEvent(String name, String description, Location location, String creatorName) {
        this.id = UUID.randomUUID().toString();
        this.name = name;
        this.description = description;
        this.location = location;
        this.creatorName = creatorName;
        this.createdAt = LocalDateTime.now();
        this.participants = new ArrayList<>();
        this.chatMessages = new ArrayList<>();
    }

    public void addParticipant(String participantName) {
        if (!participants.contains(participantName)) {
            participants.add(participantName);
        }
    }

    public void addMessage(KddChatMessage message) {
        this.chatMessages.add(message);
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public Location getLocation() { return location; }
    public String getCreatorName() { return creatorName; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public List<String> getParticipants() { return participants; }
    public List<KddChatMessage> getChatMessages() { return chatMessages; }
}
