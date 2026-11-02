package SIGA.backend.repositories;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import SIGA.backend.entities.PeriodMatriculationEntity;

@Repository 
public interface PeriodMatriculationRepository extends JpaRepository<PeriodMatriculationEntity, Long> {
    
    // find by student 
    List<PeriodMatriculationEntity> findByStudentId(Long studentId);

    // find by academic period 
    List<PeriodMatriculationEntity> findByAcademicPeriodId(Long academicPeriodId);

    // find by student and academic period
    Optional<PeriodMatriculationEntity> findByStudentIdAndAcademicPeriodId(Long studentId, Long academicPeriodId);

    // Checks if a matriculation exists for the student and academic period
    boolean existsByStudentIdAndAcademicPeriodId(Long studentId, Long academicPeriodId);

    // check if student has academic period matriculations
    boolean existsByStudentId(Long studentId);

    // check if period has matriculations
    boolean existsByAcademicPeriodId(Long academicPeriodId);
}
