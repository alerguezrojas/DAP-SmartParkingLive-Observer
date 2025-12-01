package smartparking.kdd.model;

import smartparking.kdd.observer.KddObserver;

public class KddUser implements KddObserver {
    private String id;
    private String name;
    private Location location;
    private boolean isMod;
    // In a real app, we wouldn't hold the callback here directly if it's a web client,
    // but we will use this to simulate the logic and then delegate to a WebSocket service.
    private transient java.util.function.Consumer<KddEvent> notificationCallback;

    public KddUser(String id, String name, Location location, boolean isMod) {
        this.id = id;
        this.name = name;
        this.location = location;
        this.isMod = isMod;
    }

    public void setNotificationCallback(java.util.function.Consumer<KddEvent> callback) {
        this.notificationCallback = callback;
    }

    @Override
    public void onNewEvent(KddEvent event) {
        // Logic: Check distance
        double distance = this.location.distanceTo(event.getLocation());
        if (distance <= 10.0) { // 10km radius
            if (notificationCallback != null) {
                notificationCallback.accept(event);
            }
        }
    }

    // Getters
    public String getId() { return id; }
    public String getName() { return name; }
    public Location getLocation() { return location; }
    public boolean isMod() { return isMod; }
}
