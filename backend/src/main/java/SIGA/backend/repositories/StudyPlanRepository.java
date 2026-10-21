package SIGA.backend.repositories;

import SIGA.backend.entities.StudyPlanEntity;
import SIGA.backend.entities.StudyPlanStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;


import java.util.List;
import java.util.Optional;

@Repository 
public interface StudyPlanRepository extends JpaRepository<StudyPlanEntity, Long> {
    // find by status 
    List<StudyPlanEntity> findByStatus(StudyPlanStatus status);

    // find by career id
    List<StudyPlanEntity> findByCareerId(Long careerId);

    // find by career id and identifier
    Optional<StudyPlanEntity> findByCareerIdAndIdentifier(Long careerId, String identifier);

    // exists by career id
    boolean existsByCareerId(Long careerId);

    // exists by career id and identifier 
    boolean existsByCareerIdAndIdentifier(Long careerId, String identifier);

    // find study plan by career id
    Optional<StudyPlanEntity> findByCareerIdAndStatus(Long careerId, StudyPlanStatus status);
}
