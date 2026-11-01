package SIGA.backend.repositories;

import SIGA.backend.entities.StudentEntity;
import SIGA.backend.entities.StudentStatus;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

@Repository 
public interface StudentRepository extends JpaRepository<StudentEntity, Long> {
    // find by rut
    Optional<StudentEntity> findByRut(String rut);

    // Compare existing formats without rewriting stored identifiers.
    @Query("select person from StudentEntity person where upper(replace(replace(trim(person.rut), '.', ''), '-', '')) = :rut")
    List<StudentEntity> findByNormalizedRut(@Param("rut") String rut);

    // find by email
    Optional<StudentEntity> findByEmail(String email);

    // find by status
    List<StudentEntity> findByStatus(StudentStatus status);

    // find by career id
    List<StudentEntity> findByCareerId(Long careerId);

    // find by study plan id
    List<StudentEntity> findByStudyPlanId(Long studyPlanId);

    // validations
    boolean existsByRut(String rut);
    boolean existsByEmail(String email);
    boolean existsByCareerId(Long careerId);
    boolean existsByStudyPlanId(Long studyPlanId);

    // find by name
    List<StudentEntity> findByFirstNameContainingIgnoreCase(String firstName);
    List<StudentEntity> findByLastNameContainingIgnoreCase(String lastName);

    // find by Keycloak subject
    Optional<StudentEntity> findByKeycloakSubject(String keycloakSubject);
}
