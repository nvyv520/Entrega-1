package SIGA.backend.services;

import SIGA.backend.entities.SectionEntity;
import SIGA.backend.entities.SubjectEntity;
import SIGA.backend.entities.TeacherEntity;
import SIGA.backend.entities.AcademicPeriodEntity;
import SIGA.backend.entities.AcademicPeriodStatus;
import SIGA.backend.entities.Status;
import SIGA.backend.entities.EnrollmentEntity;
import SIGA.backend.entities.EnrollmentStatus;

import SIGA.backend.repositories.TeacherRepository;
import SIGA.backend.repositories.EnrollmentRepository;
import SIGA.backend.repositories.SectionRepository;
import SIGA.backend.repositories.SubjectRepository;
import SIGA.backend.repositories.AcademicPeriodRepository;

import SIGA.backend.dtos.StudentResponseDTO;
import SIGA.backend.dtos.SectionStudentResponseDTO;
import SIGA.backend.dtos.SectionRequestDTO;
import SIGA.backend.dtos.SectionResponseDTO;

import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;

@Service
@RequiredArgsConstructor

public class SectionService {

    private final SectionRepository sectionRepository;
    private final SubjectRepository subjectRepository;
    private final TeacherRepository teacherRepository;
    private final AcademicPeriodRepository academicPeriodRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final EntityManager entityManager;


    // get all sections
    @Transactional (readOnly = true)
    public List<SectionResponseDTO> getAllSections() {
        List<SectionEntity> sections = sectionRepository.findAll();
        return sections.stream().map(SectionResponseDTO::new).toList();
    }


    // find section by id
    private SectionEntity findSectionById(Long id) {
        return sectionRepository.findById(id).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Seccion no encontrada"));
    }


    // get section by id
    @Transactional (readOnly = true)
    public SectionResponseDTO getSectionById(Long id) {
        SectionEntity section = findSectionById(id);
        return new SectionResponseDTO(section);
    }


    // get sections by subject
    @Transactional (readOnly = true)
    public List<SectionResponseDTO> getSectionsBySubject(Long subjectId) {
        List<SectionEntity> sections = sectionRepository.findBySubjectId(subjectId);
        return sections.stream().map(SectionResponseDTO::new).toList();
    }


    // get sections by academic period
    @Transactional (readOnly = true)
    public List<SectionResponseDTO> getSectionsByAcademicPeriod(Long academicPeriodId) {
        List<SectionEntity> sections = sectionRepository.findByAcademicPeriodId(academicPeriodId);
        return sections.stream().map(SectionResponseDTO::new).toList();
    }


    // get sections by teacher
    @Transactional (readOnly = true)
    public List<SectionResponseDTO> getSectionsByTeacher(Long teacherId) {
        List<SectionEntity> sections = sectionRepository.findByTeacherId(teacherId);
        return sections.stream().map(SectionResponseDTO::new).toList();
    }


    // get subjects by status 
    @Transactional (readOnly = true)
    public List<SectionResponseDTO> getSectionsByStatus(Status status) {
        List<SectionEntity> sections = sectionRepository.findByStatus(status);
        return sections.stream().map(SectionResponseDTO::new).toList();
    }


    // find subject by id 
    private SubjectEntity findSubjectById(Long id) {
        return subjectRepository.findById(id).orElseThrow(()-> new ResponseStatusException(HttpStatus.NOT_FOUND, "Asignatura no encontrada"));
    }


    // find teacher by id
    private TeacherEntity findTeacherById(Long id) {
        return teacherRepository.findById(id).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Docente no encontrado"));
    }


    // validate section data
    private void validateSectionData(SectionRequestDTO section) {

        // Checks for missing information
        if (section == null || section.getSubjectId() == null ||
            section.getAcademicPeriodId() == null || 
            section.getTeacherId() == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La asignatura, periodo academico y docente son obligatorios");
        }

        // Checks if the capacity is valid
        Integer capacity = section.getCapacity();
        if (capacity == null || capacity < 1 || capacity > 60) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La capacidad debe estar entre 1 y 60");
        }

        // Checks that at least one schedule block is assigned
        if (section.getBlocks() == null || section.getBlocks().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Debe tener al menos un bloque horario");
        }

        // Checks for duplicate schedule blocks
        if (new HashSet<>(section.getBlocks()).size() != section.getBlocks().size()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No se permiten bloques horarios duplicados");
        }

        // Checks that the schedule blocks are valid
        for (String block : section.getBlocks()) {
            if (block == null || !block.matches("^[LMWJV][1-6]$")) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El bloque horario no es valido");
            }
        }
    }


    // Validates the academic period status 
    private void validateAcademicPeriod(AcademicPeriodEntity period) {
        if (period.getStatus() != AcademicPeriodStatus.ABIERTO) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "El periodo academico debe estar abierto");
        }
    }


    // Validates the teacher status
    private void validateTeacher(TeacherEntity teacher) {
        if (teacher.getStatus() != Status.ACTIVA) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "El docente debe estar activo");
        }
    }


    // Validates the TEL hours
    private void validateHours(SubjectEntity subject, Set<String> blocks) {

        int total = subject.getLaboratoryHours() + subject.getExerciseHours() + subject.getTheoryHours();

        if (total % 2 != 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El total de " + total + " horas TEL no puede representarse exactamente mediante bloques completos de 2 horas");
        }

        // Blocks represent the total TEL; activities have no separate schedules.
        if (blocks.size() * 2 != total) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Las horas seleccionadas deben coincidir con las " + total + " horas TEL de la asignatura (" + total / 2 + " bloques de 2 horas)");
        }
    }


    // Validates teacher schedule conflicts
    private void validateTeacherSchedule(Long sectionId, Long teacherId, Long academicPeriodId, Set<String> blocks) {

        List<SectionEntity> sections = sectionRepository.findByTeacherId(teacherId);

        for (SectionEntity current : sections) {
            
            // Ignores the current section 
            if (sectionId != null && current.getId().equals(sectionId)) {
                continue;
            }

            // Checks if both sections belong to the same academic period 
            if (!current.getAcademicPeriod().getId().equals(academicPeriodId)) {
                    continue;
            }

            for (String block : blocks) {
                if (current.getBlocks().contains(block)) {
                    throw new ResponseStatusException(HttpStatus.CONFLICT, "El docente tiene un tope horario");
                }
            }
        }
    }


    // Checks if the section has enrolled students 
    private boolean hasEnrollments(Long sectionId) {
        return !enrollmentRepository.findBySectionId(sectionId).isEmpty();
    }


    // lock academic period before changing sections
    private AcademicPeriodEntity lockOpenPeriod(Long periodId) {

        AcademicPeriodEntity period = entityManager.find(AcademicPeriodEntity.class, periodId, LockModeType.PESSIMISTIC_WRITE);

        if (period == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Periodo academico no encontrado");
        }

        validateAcademicPeriod(period);

        return period;
    }



    // create section 
    @Transactional 
    public SectionResponseDTO createSection(SectionRequestDTO section) {

        // Validates the information 
        validateSectionData(section);

        Set<String> blocks = new HashSet<>(section.getBlocks());

        SubjectEntity subject = findSubjectById(section.getSubjectId());
        AcademicPeriodEntity period = lockOpenPeriod(section.getAcademicPeriodId());        TeacherEntity teacher = findTeacherById(section.getTeacherId());

        // Validates the period and teacher 
        validateAcademicPeriod(period);
        validateTeacher(teacher);

        // Checks for a duplicate section
        if (sectionRepository.existsBySubjectIdAndAcademicPeriodId(subject.getId(), period.getId())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Ya existe una seccion de esa asignatura en el periodo academico");
        }

        // Validates TEL hours and teacher schedule conflicts 
        validateHours(subject, blocks);
        validateTeacherSchedule(null, teacher.getId(), period.getId(), blocks);

        // create section 
        SectionEntity newSection = new SectionEntity();
        newSection.setSubject(subject);
        newSection.setAcademicPeriod(period);
        newSection.setTeacher(teacher);
        newSection.setCapacity(section.getCapacity());
        newSection.setEnrolledStudents(0);
        newSection.setStatus(Status.ACTIVA);
        newSection.setBlocks(blocks);

        SectionEntity saved = sectionRepository.save(newSection);
        return new SectionResponseDTO(saved);
    }


    // update section 
    @Transactional 
    public SectionResponseDTO updateSection(Long id, SectionRequestDTO section) {

        SectionEntity current = findSectionById(id);

        // Validates the information 
        validateSectionData(section);

        // lock period during update
        lockOpenPeriod(current.getAcademicPeriod().getId());
        entityManager.refresh(current);

        Set<String> blocks = new HashSet<>(section.getBlocks());

        // Prevents changes in closed periods
        validateAcademicPeriod(current.getAcademicPeriod());

        // Prevents changes to the subject or academic period 
        if (!current.getSubject().getId().equals(section.getSubjectId()) || 
            !current.getAcademicPeriod().getId().equals(section.getAcademicPeriodId())) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "No se puede cambiar la asignatura ni el periodo");
        }
        
        // Prevents changes when enrollments exist
        if (hasEnrollments(id)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "No se puede modificar ya que cuenta con inscritos");
        }

        TeacherEntity teacher = findTeacherById(section.getTeacherId());
        validateTeacher(teacher);

        // Validates the TEL hours and teacher schedule conflicts
        validateHours(current.getSubject(), blocks);
        validateTeacherSchedule(id, teacher.getId(), current.getAcademicPeriod().getId(), blocks);

        current.setTeacher(teacher);
        current.setCapacity(section.getCapacity());
        current.setBlocks(blocks);

        SectionEntity saved = sectionRepository.save(current);
        return new SectionResponseDTO(saved);
    }


    // delete section
    @Transactional 
    public void deleteSection(Long id) {

        SectionEntity section = findSectionById(id);
        lockOpenPeriod(section.getAcademicPeriod().getId());
        entityManager.refresh(section);

        // Prevents deletion if the period is closed
        validateAcademicPeriod(section.getAcademicPeriod());

        // Checks for enrollments
        if (hasEnrollments(id)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "No se puede eliminar ya que cuenta con inscritos");
        }

        sectionRepository.delete(section);
    }


    // get sections assigned to authenticated teacher
    @Transactional(readOnly = true)
    public List<SectionResponseDTO> getCurrentTeacherSections(String keycloakSubject) {

        TeacherEntity teacher = teacherRepository.findByKeycloakSubject(keycloakSubject).orElseThrow(() -> 
                                new ResponseStatusException(HttpStatus.NOT_FOUND, "No existe un docente asociado a esta cuenta"));

        List<SectionEntity> sections =
                sectionRepository.findByTeacherId(teacher.getId());

        return sections.stream().map(SectionResponseDTO::new).toList();
    }


    // get students enrolled in authenticated teacher section
    @Transactional(readOnly = true)
    public List<SectionStudentResponseDTO> getCurrentTeacherSectionStudents(String keycloakSubject, Long sectionId) {

        findCurrentTeacherSection(keycloakSubject, sectionId);

        // get active enrollments
        List<EnrollmentEntity> enrollments = enrollmentRepository.findBySectionIdAndStatus(sectionId, EnrollmentStatus.ACTIVO);

        return enrollments.stream().filter(enrollment -> enrollmentRepository.isSelectionConfirmed(
                enrollment.getStudent().getId(), enrollment.getAcademicPeriod().getId()))
                .map(SectionStudentResponseDTO::new).toList();
    }



    // resolve section only after checking the authenticated teacher assignment
    @Transactional(readOnly = true)
    public SectionEntity findCurrentTeacherSection(String keycloakSubject, Long sectionId) {

        // find authenticated teacher
        TeacherEntity teacher = teacherRepository.findByKeycloakSubject(keycloakSubject).orElseThrow(() -> 
                                new ResponseStatusException( HttpStatus.NOT_FOUND, "No existe un docente asociado a esta cuenta"));

        // find section
        SectionEntity section = findSectionById(sectionId);

        // validate teacher assignment
        if (!section.getTeacher().getId().equals(teacher.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "No tiene permisos para consultar esta seccion");
        }

        return section;
    }

}
