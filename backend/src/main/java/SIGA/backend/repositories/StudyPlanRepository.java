package SIGA.backend.repositories;

import SIGA.backend.entities.CareerEntity;
import SIGA.backend.entities.StudyPlanEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;


import java.util.List;
import java.util.Optional;

@Repository 
public interface StudyPlanRepository extends JpaRepository<StudyPlanEntity, Long> {
    // find by identifier
    Optional<StudyPlanEntity> findByIdentifier(String identifier);

    // find by status 
    List<StudyPlanEntity> findByStatus(String status);

    // find by career
    List<StudyPlanEntity> findByCareer(CareerEntity career);

    // find by career id
    List<StudyPlanEntity> findByCareerId(Long careerId);

    // exists by identifier 
    boolean existsByIdentifier(String identifier);
}