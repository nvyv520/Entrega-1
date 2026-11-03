package SIGA.backend.repositories;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import SIGA.backend.entities.EnrollmentEntity;
import SIGA.backend.entities.EnrollmentStatus;

@Repository 
public interface EnrollmentRepository extends JpaRepository<EnrollmentEntity, Long> {

    // Checks the persistent confirmation shared by the student's selection.
    @Query("select (count(m) > 0) from PeriodMatriculationEntity m where m.student.id = :studentId " +
            "and m.academicPeriod.id = :periodId and m.enrollmentConfirmed = true")
    boolean isSelectionConfirmed(@Param("studentId") Long studentId, @Param("periodId") Long periodId);

    interface SectionEnrollmentCount {
        Long getSectionId();
        Long getTotal();
    }

    @Query("select e.section.id as sectionId, count(e) as total from EnrollmentEntity e " +
            "where e.academicPeriod.id = :periodId and e.status = :status group by e.section.id")
    List<SectionEnrollmentCount> countEnrollmentsBySectionInPeriod(@Param("periodId") Long periodId,
                                                                 @Param("status") EnrollmentStatus status);
    
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
