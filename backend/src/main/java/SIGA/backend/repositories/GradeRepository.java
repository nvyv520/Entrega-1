package SIGA.backend.repositories;

import SIGA.backend.entities.GradeEntity;
import SIGA.backend.entities.GradeResult;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository 
public interface GradeRepository extends JpaRepository<GradeEntity, Long> {
    // find by enrollment id
    Optional<GradeEntity> findByEnrollmentId(Long enrollmentId);

    // find by result
    List<GradeEntity> findByResult(GradeResult result);

    // validations
    boolean existsByEnrollmentId(Long enrollmentId);

    // check if student approved a subject
    boolean existsByEnrollment_Student_IdAndEnrollment_Section_Subject_IdAndResult(
            Long studentId,
            Long subjectId,
            GradeResult result
    );
}
