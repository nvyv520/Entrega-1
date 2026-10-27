package SIGA.backend.services;

import SIGA.backend.entities.PeriodMatriculationEntity;
import SIGA.backend.entities.StudentEntity;
import SIGA.backend.entities.StudentStatus;
import SIGA.backend.entities.AcademicPeriodEntity;
import SIGA.backend.entities.AcademicPeriodStatus;

import SIGA.backend.repositories.PeriodMatriculationRepository;
import SIGA.backend.repositories.StudentRepository;
import SIGA.backend.repositories.AcademicPeriodRepository;

import SIGA.backend.dtos.PeriodMatriculationRequestDTO;
import SIGA.backend.dtos.PeriodMatriculationResponseDTO;

import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service 
@RequiredArgsConstructor 
public class PeriodMatriculationService {

    private final PeriodMatriculationRepository matriculationRepository;
    private final StudentRepository studentRepository;
    private final AcademicPeriodRepository academicPeriodRepository;


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


    // find student by id
    private StudentEntity findStudentById(Long id) {
        return studentRepository.findById(id).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Estudiante no encontrado"));
    }


    // find academic period by id
    private AcademicPeriodEntity findAcademicPeriodById(Long id) {
        return academicPeriodRepository.findById(id).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Periodo academico no encontrado"));
    }


    // create matriculation
    @Transactional 
    public PeriodMatriculationResponseDTO createMatriculation(PeriodMatriculationRequestDTO matriculation) {

        // si se ingreso una matricula vacia
        if (matriculation == null || matriculation.getStudentId() == null || matriculation.getAcademicPeriodId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "EL estudiante y periodo academico son obligatorios");
        }

        StudentEntity student = findStudentById(matriculation.getStudentId());
        AcademicPeriodEntity period = findAcademicPeriodById(matriculation.getAcademicPeriodId());

        // validar estado del estudiante (regular)
        if (student.getStatus() != StudentStatus.REGULAR) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "El estudiante no esta habilitado para matricula");
        }

        // validar estado del periodo academico 
        if (period.getStatus() != AcademicPeriodStatus.ABIERTO) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "El periodo academico no est aabierto");
        }

        // revisar que no se esta duplicando
        if (matriculationRepository.existsByStudentIdAndAcademicPeriodId(student.getId(), period.getId())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "El estudiante ya esta matriculado en este periodo");
        }

        PeriodMatriculationEntity newMatriculation = new PeriodMatriculationEntity();
        newMatriculation.setStudent(student);
        newMatriculation.setAcademicPeriod(period);

        PeriodMatriculationEntity saved = matriculationRepository.save(newMatriculation);
        return new PeriodMatriculationResponseDTO(saved);
    }
    
}
