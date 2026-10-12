package SIGA.backend.repositories;

import SIGA.backend.entities.EnrollmentEntity;
import SIGA.backend.entities.GradeEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository 
public interface GradeRepository extends JpaRepository<GradeEntity, Long> {
    // find by enrollment
    Optional<GradeEntity> findByEnrollment(EnrollmentEntity enrollment);

    // find by enrollment id
    Optional<GradeEntity> findByEnrollmentId(Long enrollmentId);

    // find by result
    List<GradeEntity> findByResult(String result);

    // validations
    boolean existsByEnrollmentId(Long enrollmentId);
}
