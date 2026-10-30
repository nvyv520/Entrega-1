package SIGA.backend.repositories;

import SIGA.backend.entities.GradeEntity;
import SIGA.backend.entities.GradeResult;
import SIGA.backend.entities.EnrollmentStatus;

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

    // find by student
    List<GradeEntity> findByEnrollment_Student_Id(Long studentId);

    // find by academic period
    List<GradeEntity> findByEnrollment_AcademicPeriod_Id(Long academicPeriodId);

    // find grades of active enrollments in a section
    List<GradeEntity> findByEnrollment_Section_IdAndEnrollment_Status(Long sectionId, EnrollmentStatus status);

    // validations
    boolean existsByEnrollmentId(Long enrollmentId);

    // check if student approved a subject
    boolean existsByEnrollment_Student_IdAndEnrollment_Section_Subject_IdAndResult( Long studentId, Long subjectId, GradeResult result);
}
