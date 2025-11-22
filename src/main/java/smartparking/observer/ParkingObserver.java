package smartparking.observer;

import smartparking.model.ParkingSpot;

/**
 * Interfaz del patrón Observador.
 * Cualquier componente que quiera reaccionar a cambios en una plaza
 * implementa esta interfaz.
 */
public interface ParkingObserver {
    void update(ParkingSpot spot);
}


