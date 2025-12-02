package smartparking.kdd.service;

import org.springframework.data.jpa.repository.JpaRepository;
import smartparking.kdd.model.KddUser;
import java.util.Optional;

public interface KddUserRepository extends JpaRepository<KddUser, String> {
    Optional<KddUser> findByName(String name);
}