package SIGA.backend.repositories;

import SIGA.backend.entities.AcademicPeriodEntity;
import SIGA.backend.entities.SectionEntity;
import SIGA.backend.entities.SubjectEntity;
import SIGA.backend.entities.TeacherEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository 
public interface SectionRepository extends JpaRepository<SectionEntity, Long> {
    // find by subject
    List<SectionEntity> findBySubject(SubjectEntity subject);

    // find by subject id
    List<SectionEntity> findBySubjectId(Long subjectId);

    // find by academic period
    List<SectionEntity> findByAcademicPeriod(AcademicPeriodEntity academicPeriod);

    // find by academic period id
    List<SectionEntity> findByAcademicPeriodId(Long academicPeriodId);

    // find by teacher
    List<SectionEntity> findByTeacher(TeacherEntity teacher);

    // find by teacher id
    List<SectionEntity> findByTeacherId(Long teacherId);

    // find by subject id and academic period id
    Optional<SectionEntity> findBySubjectIdAndAcademicPeriodId(Long subjectId, Long academicPeriodId);

    // validations
    boolean existsBySubjectIdAndAcademicPeriodId(Long subjectId, Long academicPeriodId);
}
