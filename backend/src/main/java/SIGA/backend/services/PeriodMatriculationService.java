package SIGA.backend.services;

import SIGA.backend.entities.PeriodMatriculationEntity;
import SIGA.backend.entities.StudentEntity;
import SIGA.backend.entities.StudentStatus;
import SIGA.backend.entities.AcademicPeriodEntity;
import SIGA.backend.entities.AcademicPeriodStatus;

import SIGA.backend.repositories.PeriodMatriculationRepository;
import SIGA.backend.repositories.EnrollmentRepository;
import SIGA.backend.entities.EnrollmentStatus;
import SIGA.backend.repositories.AcademicPeriodRepository;

import SIGA.backend.dtos.PeriodMatriculationRequestDTO;
import SIGA.backend.dtos.PeriodMatriculationResponseDTO;

import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;

@Service 
@RequiredArgsConstructor 
public class PeriodMatriculationService {

    private final PeriodMatriculationRepository matriculationRepository;
    private final EntityManager entityManager;
    private final EnrollmentRepository enrollmentRepository;


    // get all matriculation
    @Transactional (readOnly = true)
    public List<PeriodMatriculationResponseDTO> getAllMatriculations() {
        List<PeriodMatriculationEntity> matriculations = matriculationRepository.findAll();
        return matriculations.stream().map(PeriodMatriculationResponseDTO::new).toList();
    }


    // find matriculation by id
    private PeriodMatriculationEntity findMatriculationById(Long id) {
        return matriculationRepository.findById(id).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Matricula no encontrada"));
    }


    // get matriculation by id 
    @Transactional (readOnly = true)
    public PeriodMatriculationResponseDTO getMatriculationById(Long id) {
        PeriodMatriculationEntity matriculation = findMatriculationById(id);
        return new PeriodMatriculationResponseDTO(matriculation);
    }


    // get matriculations by student
    @Transactional (readOnly = true)
    public List<PeriodMatriculationResponseDTO> getMatriculationsByStudent(Long studentId) {
        List<PeriodMatriculationEntity> matriculations = matriculationRepository.findByStudentId(studentId);
        return matriculations.stream().map(PeriodMatriculationResponseDTO::new).toList();
    }


    // get matriculations by academic period
    @Transactional (readOnly = true)
    public List<PeriodMatriculationResponseDTO> getMatriculationsByAcademicPeriod(Long periodId) {
        List<PeriodMatriculationEntity> matriculations = matriculationRepository.findByAcademicPeriodId(periodId);
        return matriculations.stream().map(PeriodMatriculationResponseDTO::new).toList();
    }


    // create matriculation
    @Transactional 
    public PeriodMatriculationResponseDTO createMatriculation(PeriodMatriculationRequestDTO matriculation) {

        // Checks for missing matriculation information
        if (matriculation == null || matriculation.getStudentId() == null || matriculation.getAcademicPeriodId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "EL estudiante y periodo academico son obligatorios");
        }

        // lock academic period
        AcademicPeriodEntity period = entityManager.find(AcademicPeriodEntity.class, matriculation.getAcademicPeriodId(), LockModeType.PESSIMISTIC_WRITE);

        if (period == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Periodo academico no encontrado");
        }

        // lock student
        StudentEntity student = entityManager.find(StudentEntity.class, matriculation.getStudentId(), LockModeType.PESSIMISTIC_WRITE);

        if (student == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Estudiante no encontrado");
        }

        // Validates the student status (regular)
        if (student.getStatus() != StudentStatus.REGULAR) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "El estudiante no esta habilitado para matricula");
        }

        // Validates the academic period status 
        if (period.getStatus() != AcademicPeriodStatus.ABIERTO) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "El periodo academico no está abierto");
        }

        // Checks for a duplicate matriculation
        if (matriculationRepository.existsByStudentIdAndAcademicPeriodId(student.getId(), period.getId())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "El estudiante ya esta matriculado en este periodo");
        }

        PeriodMatriculationEntity newMatriculation = new PeriodMatriculationEntity();
        newMatriculation.setStudent(student);
        newMatriculation.setAcademicPeriod(period);
        newMatriculation.setEnrollmentConfirmed(false);

        PeriodMatriculationEntity saved = matriculationRepository.save(newMatriculation);
        return new PeriodMatriculationResponseDTO(saved);
    }
    
    // record the external matriculation status for an existing student
    @Transactional
    public void updateMatriculationStatus(Long studentId, Long periodId, Boolean matriculated) {
        if (studentId == null || periodId == null || matriculated == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El estudiante, período y estado de matrícula son obligatorios");
        }

        // use the same lock order as enrollment changes
        AcademicPeriodEntity period = entityManager.find(AcademicPeriodEntity.class, periodId, LockModeType.PESSIMISTIC_WRITE);
        if (period == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Período académico no encontrado");
        }
        entityManager.refresh(period);
        if (period.getStatus() != AcademicPeriodStatus.ABIERTO) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Solo se puede cambiar la matrícula en un período abierto");
        }
        StudentEntity student = entityManager.find(StudentEntity.class, studentId, LockModeType.PESSIMISTIC_WRITE);
        if (student == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Estudiante no encontrado");
        }
        entityManager.refresh(student);
        PeriodMatriculationEntity current = matriculationRepository.findByStudentIdAndAcademicPeriodId(studentId, periodId).orElse(null);
        if (current != null) {
            entityManager.refresh(current);
        }

        if (matriculated) {
            if (current == null) {
                if (student.getStatus() != StudentStatus.REGULAR) {
                    throw new ResponseStatusException(HttpStatus.CONFLICT, "El estudiante no está habilitado para matrícula");
                }
                matriculationRepository.save(new PeriodMatriculationEntity(null, student, period, false));
            }
            return;
        }

        // preserve active enrollments and their confirmed selection
        if (enrollmentRepository.findByStudentIdAndAcademicPeriodId(studentId, periodId).stream()
                .anyMatch(enrollment -> enrollment.getStatus() == EnrollmentStatus.ACTIVO)
                || (current != null && Boolean.TRUE.equals(current.getEnrollmentConfirmed()))) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "No se puede marcar sin matrícula mientras existan asignaturas inscritas o una selección confirmada");
        }
        if (current != null) {
            matriculationRepository.delete(current);
        }
    }

}
