package patmal.workshops.student.impl.logic;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import patmal.workshops.api.model.Instructor;
import patmal.workshops.api.model.Person;
import patmal.workshops.api.model.Student;
import patmal.workshops.api.model.Team;
import patmal.workshops.api.model.Workshop;
import patmal.workshops.student.impl.model.StudentImpl;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.List;

/*
 * Builds the export files as JSON trees. Every method only walks "down" from its
 * starting object, so a child never contains its parent again and there are no cycles.
 */
public class JsonExporter {
    private final ObjectMapper mapper = new ObjectMapper();

    public void exportInstructors(List<Instructor> instructors, List<Team> allTeams, File target) {
        ArrayNode root = mapper.createArrayNode();
        for (Instructor instructor : instructors) {
            ObjectNode instructorNode = personNode(instructor);
            ArrayNode workshopsNode = instructorNode.putArray("workshops");
            for (Workshop workshop : instructor.getWorkshops()) {
                ObjectNode workshopNode = workshopNode(workshop);
                ArrayNode teamsNode = workshopNode.putArray("teams");
                for (Team team : teamsOf(workshop, allTeams)) {
                    ObjectNode teamNode = teamNode(team);
                    teamNode.set("students", studentsNode(team));
                    teamsNode.add(teamNode);
                }
                workshopsNode.add(workshopNode);
            }
            root.add(instructorNode);
        }
        write(root, target);
    }

    public void exportStudents(List<StudentImpl> students, File target) {
        ArrayNode root = mapper.createArrayNode();
        for (StudentImpl student : students) {
            ObjectNode studentNode = personNode(student);
            Team team = student.getTeam();
            if (team == null) {
                studentNode.putNull("team");
            } else {
                ObjectNode teamNode = teamNode(team);
                ArrayNode ids = teamNode.putArray("studentIds");
                for (Student member : team.getStudents()) {
                    ids.add(member.getId());
                }
                teamNode.set("workshop", workshopWithInstructorNode(team.getWorkshop()));
                studentNode.set("team", teamNode);
            }
            root.add(studentNode);
        }
        write(root, target);
    }

    public void exportTeams(List<Team> teams, File target) {
        ArrayNode root = mapper.createArrayNode();
        for (Team team : teams) {
            ObjectNode teamNode = teamNode(team);
            teamNode.set("students", studentsNode(team));
            teamNode.set("workshop", workshopWithInstructorNode(team.getWorkshop()));
            root.add(teamNode);
        }
        write(root, target);
    }

    // The model has no workshop -> teams link, so the teams are filtered from the full list
    private List<Team> teamsOf(Workshop workshop, List<Team> allTeams) {
        List<Team> result = new ArrayList<>();
        for (Team team : allTeams) {
            if (team.getWorkshop().getId().equals(workshop.getId())) {
                result.add(team);
            }
        }
        return result;
    }

    private ObjectNode personNode(Person person) {
        ObjectNode node = mapper.createObjectNode();
        node.put("id", person.getId());
        node.put("name", person.getName());
        node.put("email", person.getEmail());
        return node;
    }

    private ObjectNode workshopNode(Workshop workshop) {
        ObjectNode node = mapper.createObjectNode();
        node.put("id", workshop.getId());
        node.put("name", workshop.getName());
        node.put("description", workshop.getDescription());
        return node;
    }

    private ObjectNode workshopWithInstructorNode(Workshop workshop) {
        ObjectNode node = workshopNode(workshop);
        if (workshop.getInstructor() == null) {
            node.putNull("instructor");
        } else {
            node.set("instructor", personNode(workshop.getInstructor()));
        }
        return node;
    }

    private ObjectNode teamNode(Team team) {
        ObjectNode node = mapper.createObjectNode();
        node.put("id", team.getId());
        node.put("name", team.getName());
        return node;
    }

    private ArrayNode studentsNode(Team team) {
        ArrayNode node = mapper.createArrayNode();
        for (Student student : team.getStudents()) {
            node.add(personNode(student));
        }
        return node;
    }

    private void write(ArrayNode root, File target) {
        try {
            mapper.writerWithDefaultPrettyPrinter().writeValue(target, root);
        } catch (IOException e) {
            throw new UncheckedIOException("Could not write " + target, e);
        }
    }
}
