package SIGA.backend.repositories;

import SIGA.backend.entities.AcademicPeriodEntity;
import SIGA.backend.entities.EnrollmentEntity;
import SIGA.backend.entities.SectionEntity;
import SIGA.backend.entities.StudentEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository 
public interface EnrollmentRepository extends JpaRepository<EnrollmentEntity, Long> {
    // find by student
    List<EnrollmentEntity> findByStudent(StudentEntity student);

    // find by student id
    List<EnrollmentEntity> findByStudentId(Long studentId);

    // find by section
    List<EnrollmentEntity> findBySection(SectionEntity section);

    // find by section id
    List<EnrollmentEntity> findBySectionId(Long sectionId);

    // find by academic period
    List<EnrollmentEntity> findByAcademicPeriod(AcademicPeriodEntity academicPeriod);

    // find by academic period id
    List<EnrollmentEntity> findByAcademicPeriodId(Long academicPeriodId);

    // find by status
    List<EnrollmentEntity> findByStatus(String status);

    // find by student id and academic period id
    List<EnrollmentEntity> findByStudentIdAndAcademicPeriodId(Long studentId, Long academicPeriodId);

    // validations
    boolean existsByStudentIdAndSectionId(Long studentId, Long sectionId);
}
