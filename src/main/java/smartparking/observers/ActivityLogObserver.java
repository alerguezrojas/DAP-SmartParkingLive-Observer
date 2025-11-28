package smartparking.observers;

import smartparking.model.ParkingSpot;
import smartparking.observer.ParkingObserver;
import smartparking.service.ParkingActivityLog;

/**
 * Observador que persiste los cambios de plazas en un log en memoria.
 */
public class ActivityLogObserver implements ParkingObserver {

    private final ParkingActivityLog activityLog;

    public ActivityLogObserver(ParkingActivityLog activityLog) {
        this.activityLog = activityLog;
    }

    @Override
    public void update(ParkingSpot spot) {
        activityLog.recordSpotChange(
                spot.getId(),
                spot.getStatus(),
                "core",
                "Cambio de estado"
        );
    }
}
