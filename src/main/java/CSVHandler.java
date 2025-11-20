package main.java;

import java.io.*;
import java.nio.file.*;
import java.util.*;

public class CSVHandler {

    private static final String HEADER = "ID,Name,Email,PreferredGame,SkillLevel,PreferredRole,PersonalityScore,PersonalityType";

    // Load participants from any given path
    public static List<Participant> load(Path path) throws IOException {
        List<Participant> list = new ArrayList<>();

        if (!Files.exists(path)) {
            // Only create default file if we are loading from the default location
            if (path.toString().contains("data/participants.csv")) {
                Files.createDirectories(path.getParent());
                Files.write(path, List.of(HEADER));
            }
            return list;
        }

        List<String> lines = Files.readAllLines(path);
        if (lines.size() <= 1) return list;

        for (int i = 1; i < lines.size(); i++) {
            String[] data = lines.get(i).split(",");
            // Robust check: ensure line has enough columns
            if (data.length < 8) continue;

            try {
                Participant p = new Participant(
                        data[0], data[1], data[2], data[3],
                        Integer.parseInt(data[4]),
                        Participant.Role.valueOf(data[5].toUpperCase()),
                        Integer.parseInt(data[6]),
                        Participant.PersonalityType.valueOf(data[7].toUpperCase())
                );
                list.add(p);
            } catch (IllegalArgumentException e) {
                System.err.println("Skipping corrupt line: " + lines.get(i));
            }
        }
        return list;
    }

    // Save list to path
    public static void save(Path path, List<Participant> list) throws IOException {
        List<String> lines = new ArrayList<>();
        lines.add(HEADER);
        for (Participant p : list) {
            lines.add(String.join(",",
                    p.id, p.name, p.email, p.game,
                    String.valueOf(p.skill), p.role.name(),
                    String.valueOf(p.score), p.type.name()
            ));
        }
        Files.createDirectories(path.getParent());
        Files.write(path, lines, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
    }

    // Export helper: Copies one file to another location
    public static void exportFile(Path source, Path destination) throws IOException {
        if (!Files.exists(source)) {
            throw new FileNotFoundException("Source file does not exist. Please form teams first.");
        }
        Files.createDirectories(destination.getParent());
        Files.copy(source, destination, StandardCopyOption.REPLACE_EXISTING);
    }
}