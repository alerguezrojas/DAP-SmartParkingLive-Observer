package smartparking.model;

import smartparking.observer.ParkingObserver;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Representa una plaza de aparcamiento individual.
 * Actúa como "Subject" del patrón Observer.
 */
public class ParkingSpot {

    private final int id;
    private SpotStatus status;
    private final List<ParkingObserver> observers;

    public ParkingSpot(int id) {
        this.id = id;
        this.status = SpotStatus.FREE;
        this.observers = new ArrayList<>();
    }

    public int getId() {
        return id;
    }

    public SpotStatus getStatus() {
        return status;
    }

    public void attach(ParkingObserver observer) {
        if (observer != null && !observers.contains(observer)) {
            observers.add(observer);
        }
    }

    public void detach(ParkingObserver observer) {
        observers.remove(observer);
    }

    public void setStatus(SpotStatus newStatus) {
        if (!Objects.equals(this.status, newStatus)) {
            this.status = newStatus;
            notifyObservers();
        }
    }

    private void notifyObservers() {
        for (ParkingObserver observer : observers) {
            observer.update(this);
        }
    }

    @Override
    public String toString() {
        return "ParkingSpot{id=" + id + ", status=" + status + "}";
    }
}

