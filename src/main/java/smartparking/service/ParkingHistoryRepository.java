package smartparking.service;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface ParkingHistoryRepository extends JpaRepository<ParkingHistoryEntity, Long> {
    List<ParkingHistoryEntity> findByCarparkIdOrderByTimestampAsc(String carparkId);
    Optional<ParkingHistoryEntity> findTopByCarparkIdOrderByTimestampDesc(String carparkId);
}