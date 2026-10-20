package SIGA.backend.repositories;

import SIGA.backend.entities.SubjectEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository 
public interface SubjectRepository extends JpaRepository<SubjectEntity, Long> {
    // find by name
    List<SubjectEntity> findByNameContainingIgnoreCase(String name);

    // find by semester
    List<SubjectEntity> findBySemester(Integer semester);

    // find by study plan id
    List<SubjectEntity> findByStudyPlanId(Long studyPlanId);

    // find by study plan id and code
    Optional<SubjectEntity> findByStudyPlanIdAndCode(Long studyPlanId, String code);

    // validations
    boolean existsByStudyPlanIdAndCode(Long studyPlanId, String code);
}
