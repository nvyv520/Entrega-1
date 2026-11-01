package SIGA.backend.repositories;

import SIGA.backend.entities.TeacherEntity;
import SIGA.backend.entities.Status;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

@Repository 
public interface TeacherRepository extends JpaRepository<TeacherEntity, Long> {
    // find by rut
    Optional<TeacherEntity> findByRut(String rut);

    // Compare existing formats without rewriting stored identifiers.
    @Query("select person from TeacherEntity person where upper(replace(replace(trim(person.rut), '.', ''), '-', '')) = :rut")
    List<TeacherEntity> findByNormalizedRut(@Param("rut") String rut);

    // find by email
    Optional<TeacherEntity> findByEmail(String email);

    // find by status
    List<TeacherEntity> findByStatus(Status status);

    // validations
    boolean existsByRut(String rut);
    boolean existsByEmail(String email);

    // find by name
    List<TeacherEntity> findByFirstNameContainsIgnoreCase(String firstName);
    List<TeacherEntity> findByLastNameContainsIgnoreCase(String lastName);

    // find by Keycloak subject
    Optional<TeacherEntity> findByKeycloakSubject(String keycloakSubject);

}
