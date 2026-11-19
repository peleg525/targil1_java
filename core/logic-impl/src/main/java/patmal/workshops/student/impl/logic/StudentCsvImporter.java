package patmal.workshops.student.impl.logic;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import patmal.workshops.api.logic.WorkshopSystem;
import patmal.workshops.api.model.Student;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class StudentCsvImporter {
    private static final List<String> HEADER = List.of("id", "name", "email");

    private final WorkshopSystem system;

    public StudentCsvImporter(WorkshopSystem system) {
        this.system = system;
    }

    // Stops at the first bad line; students registered before it stay in the system
    public List<Student> importFrom(File source) {
        CSVFormat format = CSVFormat.DEFAULT.builder()
                .setHeader()
                .setSkipHeaderRecord(true)
                .setIgnoreEmptyLines(true)
                .get();

        List<Student> imported = new ArrayList<>();
        try (CSVParser parser = CSVParser.parse(source, StandardCharsets.UTF_8, format)) {
            if (!HEADER.equals(parser.getHeaderNames())) {
                throw new IllegalArgumentException("Wrong header in " + source + ": " + parser.getHeaderNames());
            }
            for (CSVRecord record : parser) {
                if (record.size() != HEADER.size()) {
                    throw new IllegalArgumentException("Line " + record.getRecordNumber() + " must have 3 values: " + record.toList());
                }
                imported.add(system.createStudent(record.get(0), record.get(1), record.get(2)));
            }
        } catch (IOException e) {
            throw new UncheckedIOException("Could not read " + source, e);
        }
        return imported;
    }
}
