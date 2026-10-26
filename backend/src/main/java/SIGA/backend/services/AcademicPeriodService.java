package SIGA.backend.services;

import SIGA.backend.entities.AcademicPeriodEntity;
import SIGA.backend.entities.AcademicPeriodStatus;
import SIGA.backend.repositories.AcademicPeriodRepository;
import SIGA.backend.repositories.SectionRepository;
import SIGA.backend.repositories.EnrollmentRepository;
import SIGA.backend.repositories.PeriodMatriculationRepository;

import SIGA.backend.dtos.AcademicPeriodRequestDTO;
import SIGA.backend.dtos.AcademicPeriodResponseDTO;

import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;


@Service 
@RequiredArgsConstructor 

public class AcademicPeriodService {

    private final AcademicPeriodRepository academicPeriodRepository;
    private final SectionRepository sectionRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final PeriodMatriculationRepository periodMatriculationRepository;

    // retrieve all academic periods
    @Transactional (readOnly = true)
    public List<AcademicPeriodResponseDTO> getAllAcademicPeriods() {
        List<AcademicPeriodEntity> periods = academicPeriodRepository.findAll();
        return periods.stream().map(AcademicPeriodResponseDTO::new).toList();
    }


    // find acdemic period by id
    private AcademicPeriodEntity findAcademicPeriodById(Long id) {
        return academicPeriodRepository.findById(id).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Periodo academico no encontrado"));
    }


    // get acadmeic period by id
    @Transactional (readOnly = true)
    public AcademicPeriodResponseDTO getAcademicPeriodById(Long id) {
        AcademicPeriodEntity period =  findAcademicPeriodById(id);
        return new AcademicPeriodResponseDTO(period);
    }


    // get academic period by code
    @Transactional (readOnly = true)
    public AcademicPeriodResponseDTO getAcademicPeriodByCode(String code) {
        AcademicPeriodEntity period = academicPeriodRepository.findByCode(code).orElseThrow(() -> 
                                        new ResponseStatusException(HttpStatus.NOT_FOUND, "Periodo academico no encontrado"));
                return new AcademicPeriodResponseDTO(period);
    }


    // get acadmeic period by year
    @Transactional (readOnly = true)
    public List<AcademicPeriodResponseDTO> getAcademicPeriodsByYear(Integer year) {
        List<AcademicPeriodEntity> periods = academicPeriodRepository.findByYear(year);
        return periods.stream().map(AcademicPeriodResponseDTO::new).toList();
    }


    // get acadmeic period by status
    @Transactional (readOnly = true)
    public List<AcademicPeriodResponseDTO> getAcademicPeriodsByStatus(AcademicPeriodStatus status) {
        List<AcademicPeriodEntity> periods = academicPeriodRepository.findByStatus(status);
        return periods.stream().map(AcademicPeriodResponseDTO::new).toList();
    }


    // validate academci period data
    private void validateAcademicPeriodData(AcademicPeriodRequestDTO period) {

        // validate year
        if (period.getYear() == null || period.getYear() < 2000 || period.getYear() > 9999) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El año academico no es valido");
        }

        // validar semestre
        if (period.getSemester() == null || period.getSemester() < 1 || period.getSemester() > 2) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El semestre debe ser 1 o 2");
        }
    }


    // revisar si tiene informacion academica
    private boolean hasAcademicInformation(Long periodId) {
        return sectionRepository.existsByAcademicPeriodId(periodId) ||
                enrollmentRepository.existsByAcademicPeriodId(periodId) ||
                periodMatriculationRepository.existsByAcademicPeriodId(periodId) ;
    }


    // create academic period
    @Transactional 
    public AcademicPeriodResponseDTO createAcademicPeriod(AcademicPeriodRequestDTO period) {
        
        // validar la informacion academica
        validateAcademicPeriodData(period);

        // como la informacion esta bien continuamos 
        Integer year = period.getYear();
        Integer semester = period.getSemester();
        String code = year + "-" + semester;

        // revisar que no se este duplicando
        if (academicPeriodRepository.findByYearAndSemester(year, semester).isPresent() ||
            academicPeriodRepository.findByCode(code).isPresent()) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Ya existe un periodo academico con ese año y semestre");
        }

        AcademicPeriodEntity newPeriod = new AcademicPeriodEntity();
        newPeriod.setYear(year);
        newPeriod.setSemester(semester);
        newPeriod.setCode(code);
        newPeriod.setStatus(AcademicPeriodStatus.ABIERTO);

        AcademicPeriodEntity saved = academicPeriodRepository.save(newPeriod);
        return new AcademicPeriodResponseDTO(saved);
    }


    // update academic period
    @Transactional 
    public AcademicPeriodResponseDTO updateAcademicPeriod(Long id, AcademicPeriodRequestDTO period) {

        AcademicPeriodEntity current = findAcademicPeriodById(id);

        // validar informacion 
        validateAcademicPeriodData(period);

        // no poder acctualizar periodos cerrados
        if (current.getStatus() == AcademicPeriodStatus.CERRADO) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "No se puede modificar un periodo cerrado");
        }

        // si todo esta bien continuamos 
        Integer year = period.getYear();
        Integer semester = period.getSemester();
        String code = year + "-" + semester;

        // ver si hay cambios en el año o semestre
        boolean changes = !current.getYear().equals(year) || !current.getSemester().equals(semester);

        // no hacer cambios si hay informacion academica 
        if (changes && hasAcademicInformation(id)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "No se puede modificar un periodo academico que cuenta con informacion");
        }

        // revisar que no se este duplicando
        academicPeriodRepository.findByCode(code).ifPresent(existing -> {
            if (!existing.getId().equals(id)) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Ya existe un periodo academico con ese codigo");
            }
        });

        current.setYear(year);
        current.setSemester(semester);
        current.setCode(code);

        AcademicPeriodEntity saved = academicPeriodRepository.save(current);
        return new AcademicPeriodResponseDTO(saved);
    }


    // delete academic period
    @Transactional 
    public void deleteAcademicPeriod(Long id) {

        AcademicPeriodEntity period = findAcademicPeriodById(id);

        // no eliminar periodos cerrados 
        if (period.getStatus() == AcademicPeriodStatus.CERRADO) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "No se puede eliminar un periodo cerrado");
        }

        // ver que no tenga informacion 
        if (hasAcademicInformation(id)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "No se puede eliminar un periodo con informacion");
        }

        academicPeriodRepository.delete(period);
    }
}