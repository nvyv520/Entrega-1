package SIGA.backend.repositories;

import SIGA.backend.entities.AcademicPeriodEntity;
import SIGA.backend.entities.AcademicPeriodStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AcademicPeriodRepository extends JpaRepository<AcademicPeriodEntity, Long> {
    // find by code
    Optional<AcademicPeriodEntity> findByCode(String code);

    // find by year
    List<AcademicPeriodEntity> findByYear(Integer year);

    // find by year and semester
    Optional<AcademicPeriodEntity> findByYearAndSemester(Integer year, Integer semester);

    // find by status
    List<AcademicPeriodEntity> findByStatus(AcademicPeriodStatus status);
}
