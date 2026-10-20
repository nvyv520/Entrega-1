package SIGA.backend.repositories;

import SIGA.backend.entities.CareerEntity;
import SIGA.backend.entities.Status;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CareerRepository extends JpaRepository<CareerEntity, Long> {
    // find by code
    Optional<CareerEntity> findByCode(String code);
    
    // find by status
    List<CareerEntity> findByStatus(Status status);
    
    // exist by code
    boolean existsByCode(String code);

    // find by name
    List<CareerEntity> findByNameContainingIgnoreCase(String name);
}
