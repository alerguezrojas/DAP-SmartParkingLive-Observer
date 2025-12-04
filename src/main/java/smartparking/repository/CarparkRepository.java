package smartparking.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import smartparking.model.Carpark;

@Repository
public interface CarparkRepository extends JpaRepository<Carpark, String> {
}
