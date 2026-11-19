package patmal.workshops.student.impl.app;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import patmal.workshops.api.logic.WorkshopSystem;
import patmal.workshops.api.model.Instructor;
import patmal.workshops.api.model.Student;
import patmal.workshops.api.model.Workshop;
import patmal.workshops.student.impl.logic.WorkshopSystemImpl;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;

/*
 * workshop-cli: fills a WorkshopSystem with demo data and exports it to JSON.
 * Usage: java -jar workshop-cli.jar [students.csv]
 * Without an argument the students.csv packed in the jar is used.
 */
public class Main {
    private static final Logger log = LogManager.getLogger(Main.class);

    public static void main(String[] args) {
        WorkshopSystem system = new WorkshopSystemImpl();

        File studentsFile = args.length > 0 ? new File(args[0]) : copyBundledStudents();
        log.info("Loading students from {}", studentsFile);
        List<Student> students = system.importStudentDataFromFile(studentsFile);

        Instructor dana = system.createInstructor("i1", "Dana Levi", "dana.levi@univ.edu");
        Instructor yossi = system.createInstructor("i2", "Yossi Mizrahi", "yossi.mizrahi@univ.edu");

        Workshop java = system.createWorkshop("Java Workshop", "Core Java and object oriented design");
        Workshop web = system.createWorkshop("Web Development", "HTML, CSS and JavaScript basics");
        Workshop databases = system.createWorkshop("Databases", "SQL and relational modeling");
        system.associateWorkshopWithInstructor(dana, java);
        system.associateWorkshopWithInstructor(dana, web);
        system.associateWorkshopWithInstructor(yossi, databases);

        // The last student in the file stays free
        if (students.size() >= 5) {
            system.createTeam("Alpha", java, List.of(students.get(0).getId(), students.get(1).getId()));
            system.createTeam("Beta", web, List.of(students.get(2).getId()));
            system.createTeam("Gamma", databases, List.of(students.get(3).getId()));
        }

        File instructorsJson = new File("instructors.json");
        File studentsJson = new File("students.json");
        File teamsJson = new File("teams.json");
        system.exportInstructorsDataToJson(instructorsJson);
        system.exportStudentsDataToJson(studentsJson);
        system.exportTeamsDataToJson(teamsJson);

        log.info("Done: {} instructors, {} workshops, {} students ({} free), {} teams",
                system.getInstructors().size(), system.getWorkshops().size(), system.getStudents().size(),
                system.getFreeStudents().size(), system.getTeams().size());
        log.info("JSON files written to {}", instructorsJson.getAbsoluteFile().getParent());
    }

    private static File copyBundledStudents() {
        try (InputStream in = Main.class.getResourceAsStream("/students.csv")) {
            if (in == null) {
                throw new IllegalStateException("students.csv is missing from the jar");
            }
            Path temp = Files.createTempFile("students", ".csv");
            temp.toFile().deleteOnExit();
            Files.copy(in, temp, StandardCopyOption.REPLACE_EXISTING);
            return temp.toFile();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
