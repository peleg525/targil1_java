package patmal.workshops.student.impl.logic;

import org.apache.commons.validator.routines.EmailValidator;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import patmal.workshops.api.logic.WorkshopSystem;
import patmal.workshops.api.model.Instructor;
import patmal.workshops.api.model.Student;
import patmal.workshops.api.model.Team;
import patmal.workshops.api.model.Workshop;
import patmal.workshops.student.impl.model.InstructorImpl;
import patmal.workshops.student.impl.model.StudentImpl;
import patmal.workshops.student.impl.model.TeamImpl;
import patmal.workshops.student.impl.model.WorkshopImpl;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/*
 * Unknown keys: getX(id) returns null, getStudentsOfX(id) returns an empty list.
 * No exception is thrown for lookups.
 */
public class WorkshopSystemImpl implements WorkshopSystem {
    private static final Logger log = LogManager.getLogger(WorkshopSystemImpl.class);

    private final Map<String, InstructorImpl> instructors = new LinkedHashMap<>();
    private final Map<String, StudentImpl> students = new LinkedHashMap<>();
    private final Map<Integer, WorkshopImpl> workshops = new LinkedHashMap<>();
    private final Map<Integer, TeamImpl> teams = new LinkedHashMap<>();

    private int nextWorkshopId = 1;
    private int nextTeamId = 1;

    private final JsonExporter jsonExporter = new JsonExporter();

    public WorkshopSystemImpl() {
    }

    @Override
    public Instructor createInstructor(String id, String name, String email) {
        if (!isValidEmail(email)) {
            throw rejected("Invalid e-mail for instructor " + id + ": " + email);
        }
        if (instructors.containsKey(id)) {
            throw rejected("Instructor already exists: " + id);
        }
        InstructorImpl instructor = new InstructorImpl(id, name, email);
        instructors.put(id, instructor);
        log.info("Created instructor {} ({})", id, name);
        return instructor;
    }

    @Override
    public Student createStudent(String id, String name, String email) {
        if (!isValidEmail(email)) {
            throw rejected("Invalid e-mail for student " + id + ": " + email);
        }
        if (students.containsKey(id)) {
            throw rejected("Student already exists: " + id);
        }
        StudentImpl student = new StudentImpl(id, name, email);
        students.put(id, student);
        log.info("Created student {} ({})", id, name);
        return student;
    }

    @Override
    public Workshop createWorkshop(String name, String description) {
        WorkshopImpl workshop = new WorkshopImpl(nextWorkshopId, name, description);
        workshops.put(workshop.getId(), workshop);
        nextWorkshopId++;
        log.info("Created workshop {} ({})", workshop.getId(), name);
        return workshop;
    }

    @Override
    public void associateWorkshopWithInstructor(Instructor instructor, Workshop workshop) {
        InstructorImpl newInstructor = instructors.get(instructor.getId());
        WorkshopImpl ws = workshops.get(workshop.getId());
        if (newInstructor == null || ws == null) {
            throw rejected("Unknown instructor or workshop");
        }

        // A workshop has one instructor, so detach it from the previous one first
        InstructorImpl oldInstructor = ws.getInstructor() == null ? null : instructors.get(ws.getInstructor().getId());
        if (oldInstructor != null) {
            oldInstructor.removeWorkshop(ws);
        }
        ws.setInstructor(newInstructor);
        newInstructor.addWorkshop(ws);
        log.info("Workshop {} is now taught by instructor {}", ws.getId(), newInstructor.getId());
    }

    @Override
    public Team createTeam(String name, Workshop workshop, List<String> members) {
        WorkshopImpl ws = workshops.get(workshop.getId());
        if (ws == null) {
            throw rejected("Unknown workshop: " + workshop.getId());
        }

        // Validate all members before changing anything, so a rejected call leaves no trace
        List<Student> teamStudents = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        for (String studentId : members) {
            StudentImpl student = students.get(studentId);
            if (student == null) {
                throw rejected("Unknown student: " + studentId);
            }
            if (!seen.add(studentId)) {
                throw rejected("Student listed twice: " + studentId);
            }
            if (student.getTeam() != null) {
                throw rejected("Student already in a team: " + studentId);
            }
            teamStudents.add(student);
        }

        TeamImpl team = new TeamImpl(nextTeamId, name, ws, teamStudents);
        nextTeamId++;
        teams.put(team.getId(), team);
        for (Student student : teamStudents) {
            ((StudentImpl) student).setTeam(team);
        }
        log.info("Created team {} ({}) in workshop {} with students {}", team.getId(), name, ws.getId(), members);
        return team;
    }

    @Override
    public List<Instructor> getInstructors() {
        return new ArrayList<>(instructors.values());
    }

    @Override
    public Instructor getInstructor(String id) {
        return instructors.get(id);
    }

    @Override
    public List<Student> getStudents() {
        return new ArrayList<>(students.values());
    }

    @Override
    public Student getStudent(String id) {
        return students.get(id);
    }

    @Override
    public List<Workshop> getWorkshops() {
        return new ArrayList<>(workshops.values());
    }

    @Override
    public Workshop getWorkshop(Integer id) {
        return workshops.get(id);
    }

    @Override
    public List<Team> getTeams() {
        return new ArrayList<>(teams.values());
    }

    @Override
    public Team getTeam(Integer id) {
        return teams.get(id);
    }

    @Override
    public List<Student> getFreeStudents() {
        List<Student> free = new ArrayList<>();
        for (StudentImpl student : students.values()) {
            if (student.getTeam() == null) {
                free.add(student);
            }
        }
        return free;
    }

    @Override
    public List<Student> getStudentsOfInstructor(String instructorId) {
        List<Student> result = new ArrayList<>();
        InstructorImpl instructor = instructors.get(instructorId);
        if (instructor == null) {
            return result;
        }
        for (Workshop workshop : instructor.getWorkshops()) {
            result.addAll(getStudentsOfWorkshop(workshop.getId()));
        }
        return result;
    }

    @Override
    public List<Student> getStudentsOfWorkshop(Integer workshopId) {
        List<Student> result = new ArrayList<>();
        for (TeamImpl team : teams.values()) {
            if (team.getWorkshop().getId().equals(workshopId)) {
                result.addAll(team.getStudents());
            }
        }
        return result;
    }

    @Override
    public void exportInstructorsDataToJson(File targetFileName) {
        jsonExporter.exportInstructors(getInstructors(), getTeams(), targetFileName);
        log.info("Exported {} instructors to {}", instructors.size(), targetFileName);
    }

    @Override
    public void exportStudentsDataToJson(File targetFileName) {
        jsonExporter.exportStudents(new ArrayList<>(students.values()), targetFileName);
        log.info("Exported {} students to {}", students.size(), targetFileName);
    }

    @Override
    public void exportTeamsDataToJson(File targetFileName) {
        jsonExporter.exportTeams(getTeams(), targetFileName);
        log.info("Exported {} teams to {}", teams.size(), targetFileName);
    }

    @Override
    public List<Student> importStudentDataFromFile(File source) {
        List<Student> imported = new StudentCsvImporter(this).importFrom(source);
        log.info("Imported {} students from {}", imported.size(), source);
        return imported;
    }

    private boolean isValidEmail(String email) {
        return EmailValidator.getInstance().isValid(email);
    }

    private IllegalArgumentException rejected(String message) {
        log.warn("Rejected: {}", message);
        return new IllegalArgumentException(message);
    }
}
