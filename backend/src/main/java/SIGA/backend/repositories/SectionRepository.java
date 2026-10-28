package SIGA.backend.repositories;

import SIGA.backend.entities.AcademicPeriodStatus;
import SIGA.backend.entities.SectionEntity;
import SIGA.backend.entities.Status;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository 
public interface SectionRepository extends JpaRepository<SectionEntity, Long> {
    // find by subject id
    List<SectionEntity> findBySubjectId(Long subjectId);

    // find by academic period id
    List<SectionEntity> findByAcademicPeriodId(Long academicPeriodId);

    // find by teacher id
    List<SectionEntity> findByTeacherId(Long teacherId);

    // find by status
    List<SectionEntity> findByStatus(Status status);

    // find by subject id and academic period id
    Optional<SectionEntity> findBySubjectIdAndAcademicPeriodId(Long subjectId, Long academicPeriodId);

    // validations
    boolean existsBySubjectIdAndAcademicPeriodId(Long subjectId, Long academicPeriodId);

    // check if subject has associated sections
    boolean existsBySubjectId(Long subjectId);

    // check if teacher has associated sections
    boolean existsByTeacherId(Long teacherId);

    // check if teacher has sections in open academic periods
    boolean existsByTeacherIdAndAcademicPeriod_Status(Long teacherId, AcademicPeriodStatus status);

    // check if period has sections
    boolean existsByAcademicPeriodId(Long academicPeriodId);
    
}
