package SIGA.backend.repositories;

import SIGA.backend.entities.CareerEntity;
import SIGA.backend.entities.StudentEntity;
import SIGA.backend.entities.StudyPlanEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository 
public interface StudentRepository extends JpaRepository<StudentEntity, Long> {
    // find by rut
    Optional<StudentEntity> findByRut(String rut);

    // find by email
    Optional<StudentEntity> findByEmail(String email);

    // find by status
    List<StudentEntity> findByStatus(String status);

    // find by career
    List<StudentEntity> findByCareer(CareerEntity career);

    // find by career id
    List<StudentEntity> findByCareerId(Long careerId);

    // find by study plan
    List<StudentEntity> findByStudyPlan(StudyPlanEntity studyPlan);

    // find by study plan id
    List<StudentEntity> findByStudyPlanId(Long studyPlanId);

    // validations
    boolean existsByRut(String rut);
    boolean existsByEmail(String email);

    // find by name
    List<StudentEntity> findByFirstNameContainingIgnoreCase(String firstName);
    List<StudentEntity> findByLastNameContainingIgnoreCase(String lastName);
}
