package main.java;

import java.io.*;
import java.nio.file.*;
import java.util.*;

public class CSVHandler {

    private static final String HEADER = "ID,Name,Email,PreferredGame,SkillLevel,PreferredRole,PersonalityScore,PersonalityType";

    // Load participants (Standard)
    public static List<Participant> load(Path path) throws IOException {
        List<Participant> list = new ArrayList<>();

        if (!Files.exists(path)) {
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

    // Save Participants (Standard)
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


    // Load Raw Lines (For Viewing)

    public static List<String> loadRaw(Path path) throws IOException {
        if (!Files.exists(path)) {
            return new ArrayList<>();
        }
        return Files.readAllLines(path);
    }


    // Save Teams (Sorted by Skill DESCENDING)

    public static void saveTeams(Path path, List<Team> teams) throws IOException {

        // 1. SORTING - Sort teams by Average Skill (Highest to Lowest)
        teams.sort(Comparator.comparingDouble(Team::getAverageSkill).reversed());

        List<String> lines = new ArrayList<>();
        lines.add("TeamID,PlayerID,Name,Game,Skill,Role,PersonalityType");

        for (Team t : teams) {
            // Write individual members
            for (Participant p : t.members) {
                lines.add(String.join(",",
                        "Team " + t.id,
                        p.id,
                        p.name,
                        p.game,
                        String.valueOf(p.skill),
                        p.role.name(),
                        p.type.name()
                ));
            }

            // Write Team Summary Row
            if (!t.members.isEmpty()) {
                double avgSkill = t.getAverageSkill();
                int uniqueRoles = t.getUniqueRoleCount();
                long leaders = t.members.stream().filter(m -> m.type == Participant.PersonalityType.LEADER).count();
                long balanced = t.members.stream().filter(m -> m.type == Participant.PersonalityType.BALANCED).count();
                long thinkers = t.members.stream().filter(m -> m.type == Participant.PersonalityType.THINKER).count();

                lines.add(String.join(",",
                        "Team " + t.id + " SUMMARY",
                        "",
                        "",
                        "Avg Skill: " + String.format("%.1f", avgSkill),
                        "Unique Roles: " + uniqueRoles,
                        "Comp: " + leaders + "L / " + thinkers + "T / " + balanced + "B",
                        ""
                ));
            }
            lines.add(""); // Empty line for readability
        }

        Files.createDirectories(path.getParent());
        Files.write(path, lines, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
    }

    // Export helper
    public static void exportFile(Path source, Path destination) throws IOException {
        if (!Files.exists(source)) {
            throw new FileNotFoundException("Source file does not exist. Please form teams first.");
        }
        Files.createDirectories(destination.getParent());
        Files.copy(source, destination, StandardCopyOption.REPLACE_EXISTING);
    }
}