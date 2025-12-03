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

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(name = "kdd_user_subscriptions", joinColumns = @JoinColumn(name = "mod_id"), inverseJoinColumns = @JoinColumn(name = "subscriber_id"))
    @com.fasterxml.jackson.annotation.JsonIgnoreProperties("subscriptions") // Evitar recursión en la serialización de
                                                                            // suscriptores
    private java.util.List<KddUser> subscribers = new java.util.ArrayList<>();

    @ManyToMany(mappedBy = "subscribers", fetch = FetchType.EAGER)
    @com.fasterxml.jackson.annotation.JsonIgnoreProperties({ "subscribers", "subscriptions", "notificationCallback" })
    private java.util.List<KddUser> subscriptions = new java.util.ArrayList<>();

    @Transient // No guardar el callback en la base de datos
    private transient java.util.function.Consumer<KddEvent> notificationCallback;

    public KddUser() {
    }

    public KddUser(String id, String name, Location location, boolean isMod) {
        this.id = (id == null) ? UUID.randomUUID().toString() : id;
        this.name = name;
        this.location = location;
        this.isMod = isMod;
    }

    public void setNotificationCallback(java.util.function.Consumer<KddEvent> callback) {
        this.notificationCallback = callback;
    }

    public void addSubscriber(KddUser user) {
        if (!subscribers.contains(user)) {
            subscribers.add(user);
        }
    }

    public void removeSubscriber(KddUser user) {
        subscribers.remove(user);
    }

    public java.util.List<KddUser> getSubscriptions() {
        return subscriptions;
    }

    public void notifySubscribers(KddEvent event) {
        for (KddUser subscriber : subscribers) {
            subscriber.onNewEvent(event);
        }
    }

    @Override
    public void onNewEvent(KddEvent event) {
        // Ya no verificamos distancia aquí, porque se supone que si estamos suscritos
        // es porque estamos en la zona (o nos interesa).
        if (notificationCallback != null) {
            notificationCallback.accept(event);
        }
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public Location getLocation() {
        return location;
    }

    public boolean isMod() {
        return isMod;
    }

    public void setLocation(Location location) {
        this.location = location;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        KddUser kddUser = (KddUser) o;
        return java.util.Objects.equals(id, kddUser.id);
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(id);
    }
}
