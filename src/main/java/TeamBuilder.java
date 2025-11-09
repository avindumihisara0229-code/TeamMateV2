package main.java;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;

public class TeamBuilder {

    public static List<Team> build(List<Participant> players, int size) throws InterruptedException {
        if (players.isEmpty()) throw new IllegalArgumentException("No players available.");
        Collections.shuffle(players);
        int totalTeams = (int)Math.ceil(players.size() / (double)size);
        List<Team> teams = new ArrayList<>();
        for (int i = 0; i < totalTeams; i++) teams.add(new Team(i + 1));

        ExecutorService ex = Executors.newFixedThreadPool(Math.min(totalTeams, 4));
        for (Team t : teams) {
            ex.submit(() -> {
                synchronized (players) {
                    while (t.members.size() < size && !players.isEmpty()) {
                        t.members.add(players.remove(0));
                    }
                }
            });
        }
        ex.shutdown();
        ex.awaitTermination(5, TimeUnit.SECONDS);
        return teams;
    }

    public static void saveTeams(Path path, List<Team> teams) throws IOException {
        List<String> lines = new ArrayList<>();
        lines.add("TeamID,PlayerID,Name,Role,Game,Skill,PersonalityType");

        for (Team t : teams) {
            for (Participant p : t.members) {
                lines.add(String.join(",",
                        "Team " + t.id,
                        p.id,
                        p.name,
                        p.role.name(),
                        p.game,
                        String.valueOf(p.skill),
                        p.type.name()
                ));
            }
            lines.add(""); // blank line between teams
        }

        Files.createDirectories(path.getParent());
        Files.write(path, lines, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
    }
}
