package SIGA.backend.repositories;

import SIGA.backend.entities.EnrollmentEntity;
import SIGA.backend.entities.EnrollmentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository 
public interface EnrollmentRepository extends JpaRepository<EnrollmentEntity, Long> {
    // find by student id
    List<EnrollmentEntity> findByStudentId(Long studentId);

    // find by section id
    List<EnrollmentEntity> findBySectionId(Long sectionId);

    // find by academic period id
    List<EnrollmentEntity> findByAcademicPeriodId(Long academicPeriodId);

    // find by status
    List<EnrollmentEntity> findByStatus(EnrollmentStatus status);

    // find by student id and academic period id
    List<EnrollmentEntity> findByStudentIdAndAcademicPeriodId(Long studentId, Long academicPeriodId);

    // validations
    boolean existsByStudentIdAndSectionId(Long studentId, Long sectionId);
}
