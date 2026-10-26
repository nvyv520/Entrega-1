package SIGA.backend.repositories;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import SIGA.backend.entities.PeriodMatriculationEntity;

@Repository 
public interface PeriodMatriculationRepository extends JpaRepository<PeriodMatriculationEntity, Long> {
    // find by student id
    List<PeriodMatriculationEntity> findByStudentId(Long studentId);

    // find by academic period id
    List<PeriodMatriculationEntity> findByAcademicPeriodId(Long academicPeriodId);

    // validations
    boolean existsByStudentIdAndAcademicPeriodId(Long studentId, Long academicPeriodId);

    // check if student has academic period matriculations
    boolean existsByStudentId(Long studentId);

    // check if period has matriculations
    boolean existsByAcademicPeriodId(Long academicPeriodId);
}
