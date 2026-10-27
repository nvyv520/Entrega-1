package SIGA.backend.repositories;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import SIGA.backend.entities.EnrollmentEntity;
import SIGA.backend.entities.EnrollmentStatus;

@Repository 
public interface EnrollmentRepository extends JpaRepository<EnrollmentEntity, Long> {
    
    // find by student 
    List<EnrollmentEntity> findByStudentId(Long studentId);

    // find by section
    List<EnrollmentEntity> findBySectionId(Long sectionId);

    // find by academic period
    List<EnrollmentEntity> findByAcademicPeriodId(Long academicPeriodId);

    // find by status
    List<EnrollmentEntity> findByStatus(EnrollmentStatus status);

    // find by student and academic period
    List<EnrollmentEntity> findByStudentIdAndAcademicPeriodId(Long studentId, Long academicPeriodId);

    // find student enrollment in section
    Optional<EnrollmentEntity> findByStudentIdAndSectionId(Long studentId, Long sectionId);

    // find active enrollments in section
    List<EnrollmentEntity> findBySectionIdAndStatus(Long sectionId, EnrollmentStatus status);

    // check by student and section
    boolean existsByStudentIdAndSectionId(Long studentId, Long sectionId);

    // check enrollments associated with a subject
    boolean existsBySection_Subject_Id(Long subjectId);

    // check if student has enrollments
    boolean existsByStudentId(Long studentId);

    // check if period has enrollments
    boolean existsByAcademicPeriodId(Long academicPeriodId);

    // check active enrollments in section
    boolean existsBySectionIdAndStatus(Long sectionId, EnrollmentStatus status);

    // count active enrollments in section
    long countBySectionIdAndStatus(Long sectionId, EnrollmentStatus status);
}
