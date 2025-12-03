package smartparking.kdd.model;

import jakarta.persistence.*;
import smartparking.kdd.observer.KddObserver;
import java.util.UUID;

@Entity
@Table(name = "kdd_users")
public class KddUser implements KddObserver {
    
    @Id
    private String id;
    private String name;
    
    @Embedded
    private Location location;
    
    private boolean isMod;

    @Transient // No guardar el callback en la base de datos
    private transient java.util.function.Consumer<KddEvent> notificationCallback;

    public KddUser() {}

    public KddUser(String id, String name, Location location, boolean isMod) {
        this.id = (id == null) ? UUID.randomUUID().toString() : id;
        this.name = name;
        this.location = location;
        this.isMod = isMod;
    }

    public void setNotificationCallback(java.util.function.Consumer<KddEvent> callback) {
        this.notificationCallback = callback;
    }

    @Override
    public void onNewEvent(KddEvent event) {
        double distance = this.location.distanceTo(event.getLocation());
        if (distance <= 10.0) { 
            if (notificationCallback != null) {
                notificationCallback.accept(event);
            }
        }
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public Location getLocation() { return location; }
    public boolean isMod() { return isMod; }

    public void setLocation(Location location) {
        this.location = location;
    }
}
