package smartparking.service;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "parking_history")
public class ParkingHistoryEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String carparkId;
    private LocalDateTime timestamp;
    private int free;
    private int occupied;

    public ParkingHistoryEntity() {}

    public ParkingHistoryEntity(String carparkId, LocalDateTime timestamp, int free, int occupied) {
        this.carparkId = carparkId;
        this.timestamp = timestamp;
        this.free = free;
        this.occupied = occupied;
    }

    public Long getId() { return id; }
    public String getCarparkId() { return carparkId; }
    public LocalDateTime getTimestamp() { return timestamp; }
    public int getFree() { return free; }
    public int getOccupied() { return occupied; }
}