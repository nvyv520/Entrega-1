package SIGA.backend.repositories;

import SIGA.backend.entities.TeacherEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository 
public interface TeacherRepository extends JpaRepository<TeacherEntity, Long> {
    // find by rut
    Optional<TeacherEntity> findByRut(String rut);

    // find by email
    Optional<TeacherEntity> findByEmail(String email);

    // find by status
    List<TeacherEntity> findByStatus(String status);

    // validations
    boolean existByRut(String rut);
    boolean existByEmail(String email);

    // find by name
    List<TeacherEntity> findByFirstNameContainsIgnoreCase(String firstName);
    List<TeacherEntity> findByLastNameContainsIgnoreCase(String lastName);


}