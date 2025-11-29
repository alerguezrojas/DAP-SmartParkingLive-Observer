package smartparking.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "parking")
public class ParkingProperties {

    /**
     * Número de plazas a mostrar en la visualización.
     */
    private int displaySpots = 20;

    /**
     * Si es true, ajusta el tamaño del parking al feed externo.
     */
    private boolean mirrorFeedSize = false;

    /**
     * Número máximo de plazas permitidas (0 = sin límite).
     */
    private int maxSpots = 0;

    /**
     * Número del parking en la API de Singapur.
     */
    private String carparkNumber = "";

    /**
     * Intervalo de actualización en milisegundos.
     */
    private long updateIntervalMs = 30000;

    public int getDisplaySpots() {
        return displaySpots;
    }

    public void setDisplaySpots(int displaySpots) {
        this.displaySpots = displaySpots;
    }

    public boolean isMirrorFeedSize() {
        return mirrorFeedSize;
    }

    public void setMirrorFeedSize(boolean mirrorFeedSize) {
        this.mirrorFeedSize = mirrorFeedSize;
    }

    public int getMaxSpots() {
        return maxSpots;
    }

    public void setMaxSpots(int maxSpots) {
        this.maxSpots = maxSpots;
    }

    public String getCarparkNumber() {
        return carparkNumber;
    }

    public void setCarparkNumber(String carparkNumber) {
        this.carparkNumber = carparkNumber;
    }

    public long getUpdateIntervalMs() {
        return updateIntervalMs;
    }

    public void setUpdateIntervalMs(long updateIntervalMs) {
        this.updateIntervalMs = updateIntervalMs;
    }
}
