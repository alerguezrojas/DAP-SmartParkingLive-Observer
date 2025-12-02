package smartparking.kdd.service;

import org.springframework.data.jpa.repository.JpaRepository;
import smartparking.kdd.model.KddEvent;

public interface KddEventRepository extends JpaRepository<KddEvent, String> {
}