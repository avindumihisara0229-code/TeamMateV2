package main.java;

import java.io.*;
import java.nio.file.*;
import java.util.*;

public class CSVHandler {

    private static final String HEADER = "ID,Name,Email,PreferredGame,SkillLevel,PreferredRole,PersonalityScore,PersonalityType";

    public static List<Participant> load(Path path) throws IOException {
        List<Participant> list = new ArrayList<>();

        if (!Files.exists(path)) {
            Files.createDirectories(path.getParent());
            Files.write(path, List.of(HEADER));
            return list;
        }

        List<String> lines = Files.readAllLines(path);
        if (lines.size() <= 1) return list; // skip empty

        for (int i = 1; i < lines.size(); i++) {
            String[] data = lines.get(i).split(",");
            if (data.length < 8) continue;
            Participant p = new Participant(
                    data[0],
                    data[1],
                    data[2],
                    data[3],
                    Integer.parseInt(data[4]),
                    Participant.Role.valueOf(data[5].toUpperCase()),
                    Integer.parseInt(data[6]),
                    Participant.PersonalityType.valueOf(data[7].toUpperCase())
            );
            list.add(p);
        }
        return list;
    }

    public static void save(Path path, List<Participant> list) throws IOException {
        List<String> lines = new ArrayList<>();
        lines.add(HEADER);
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

        Files.createDirectories(path.getParent());
        Files.write(path, lines, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
    }
}
