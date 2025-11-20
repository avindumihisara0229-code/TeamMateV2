package main.java;

import java.io.*;
import java.nio.file.*;
import java.util.*;

public class CSVHandler {

    // Define the CSV column structure
    private static final String HEADER = "ID,Name,Email,PreferredGame,SkillLevel,PreferredRole,PersonalityScore,PersonalityType";

    // Load participants from CSV into a List
    public static List<Participant> load(Path path) throws IOException {
        List<Participant> list = new ArrayList<>();

        // Check if file exists; if not, initialize it with headers
        if (!Files.exists(path)) {
            Files.createDirectories(path.getParent()); // Create folder if missing
            Files.write(path, List.of(HEADER));
            return list; // Return empty list for a new file
        }

        List<String> lines = Files.readAllLines(path);

        // If file has only the header or is empty, return empty list
        if (lines.size() <= 1) return list;

        // Start loop at 1 to skip the CSV Header row
        for (int i = 1; i < lines.size(); i++) {
            String[] data = lines.get(i).split(",");

            // Basic validation to ensure row has all columns
            if (data.length < 8) continue;

            // Parse data and instantiate Participant object
            Participant p = new Participant(
                    data[0], // ID
                    data[1], // Name
                    data[2], // Email
                    data[3], // Game
                    Integer.parseInt(data[4]), // Skill
                    Participant.Role.valueOf(data[5].toUpperCase()), // Role Enum
                    Integer.parseInt(data[6]), // Score
                    Participant.PersonalityType.valueOf(data[7].toUpperCase()) // Personality Enum
            );
            list.add(p);
        }
        return list;
    }

    // Save the list of participants back to CSV
    public static void save(Path path, List<Participant> list) throws IOException {
        List<String> lines = new ArrayList<>();

        // Always write the header first
        lines.add(HEADER);

        // Convert each Participant object into a CSV-formatted string
        for (Participant p : list) {
            lines.add(String.join(",",
                    p.id,
                    p.name,
                    p.email,
                    p.game,
                    String.valueOf(p.skill),
                    p.role.name(),
                    String.valueOf(p.score),
                    p.type.name()
            ));
        }

        // Ensure parent directory exists
        Files.createDirectories(path.getParent());

        // Write all lines to file, overwriting existing content (TRUNCATE_EXISTING)
        Files.write(path, lines, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
    }
}