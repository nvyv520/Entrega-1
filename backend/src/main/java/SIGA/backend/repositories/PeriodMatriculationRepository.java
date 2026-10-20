package SIGA.backend.repositories;

import SIGA.backend.entities.PeriodMatriculationEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository 
public interface PeriodMatriculationRepository extends JpaRepository<PeriodMatriculationEntity, Long> {
    // find by student id
    List<PeriodMatriculationEntity> findByStudentId(Long studentId);

    // find by academic period id
    List<PeriodMatriculationEntity> findByAcademicPeriodId(Long academicPeriodId);

    // validations
    boolean existsByStudentIdAndAcademicPeriodId(Long studentId, Long academicPeriodId);
}
