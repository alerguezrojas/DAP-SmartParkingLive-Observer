package smartparking.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import smartparking.model.ParkingEvent;
import smartparking.model.SpotStatus;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Optional;

/**
 * Registro en memoria de eventos del parking, consumido por APIs y monitorizacion.
 */
@Component
public class ParkingActivityLog {

    private final Deque<ParkingEvent> events = new ArrayDeque<>();
    private final int maxSize;
    private final Clock clock;

    public ParkingActivityLog(@Value("${parking.activity-log.max-size:200}") int maxSize) {
        this.maxSize = Math.max(10, maxSize);
        this.clock = Clock.systemUTC();
    }

    public void recordSpotChange(int spotId, SpotStatus status, String source, String message) {
        record(new ParkingEvent(spotId, status, Instant.now(clock), source, message));
    }

    public synchronized void record(ParkingEvent event) {
        events.addFirst(event);
        while (events.size() > maxSize) {
            events.removeLast();
        }
    }

    public synchronized List<ParkingEvent> getRecent(int limit) {
        int safeLimit = Math.max(1, Math.min(limit, maxSize));
        List<ParkingEvent> snapshot = new ArrayList<>(events);
        return snapshot.subList(0, Math.min(safeLimit, snapshot.size()));
    }

    public synchronized long countSince(Instant since) {
        return events.stream()
                .filter(ev -> !ev.occurredAt().isBefore(since))
                .count();
    }

    public synchronized Optional<Instant> latestEventTime() {
        return events.isEmpty() ? Optional.empty() : Optional.of(events.peekFirst().occurredAt());
    }
}
