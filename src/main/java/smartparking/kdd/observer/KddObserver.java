package smartparking.kdd.observer;

import smartparking.kdd.model.KddEvent;

public interface KddObserver {
    void onNewEvent(KddEvent event);
}
