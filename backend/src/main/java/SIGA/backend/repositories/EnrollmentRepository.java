package SIGA.backend.repositories;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import SIGA.backend.entities.EnrollmentEntity;
import SIGA.backend.entities.EnrollmentStatus;

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

    // check enrollments associated with a subject
    boolean existsBySection_Subject_Id(Long subjectId);

    // check if student has enrollments
    boolean existsByStudentId(Long studentId);

    // check if period has enrollments
    boolean existsByAcademicPeriodId(Long academicPeriodId);
}
