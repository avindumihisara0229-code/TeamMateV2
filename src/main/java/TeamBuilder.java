package main.java;

import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;

public class TeamBuilder {

    public static List<Team> build(List<Participant> players, int teamSize) throws InterruptedException {
        if (players.isEmpty()) throw new IllegalArgumentException("No players available.");
        Collections.shuffle(players);

        int totalTeams = (int) Math.ceil(players.size() / (double) teamSize);
        List<Team> teams = new ArrayList<>();
        for (int i = 0; i < totalTeams; i++) teams.add(new Team(i + 1));

        ExecutorService ex = Executors.newFixedThreadPool(Math.min(totalTeams, 4));
        for (Team team : teams) {
            ex.submit(() -> allocateBalancedTeam(players, team, teamSize));
        }
        ex.shutdown();
        ex.awaitTermination(5, TimeUnit.SECONDS);

        balanceSkillLevels(teams);
        return teams;
    }

    private static synchronized void allocateBalancedTeam(List<Participant> pool, Team team, int teamSize) {
        Map<String, Long> gameCount = new HashMap<>();
        Set<Participant.Role> roles = new HashSet<>();

        while (team.members.size() < teamSize && !pool.isEmpty()) {
            Participant p = pool.remove(0);
            long count = gameCount.getOrDefault(p.game, 0L);

            if (count < 2) { // Game cap: max 2 per game
                team.members.add(p);
                gameCount.put(p.game, count + 1);
                roles.add(p.role);
            }

            if (roles.size() >= 3 && team.members.size() >= teamSize) break;
        }

        if (team.members.stream().noneMatch(m -> m.type == Participant.PersonalityType.LEADER)) {
            pool.stream()
                    .filter(m -> m.type == Participant.PersonalityType.LEADER)
                    .findFirst()
                    .ifPresent(m -> {
                        team.members.add(m);
                        pool.remove(m);
                    });
        }
    }

    private static void balanceSkillLevels(List<Team> teams) {
        double avg = teams.stream()
                .mapToDouble(t -> t.members.stream().mapToInt(m -> m.skill).average().orElse(0))
                .average().orElse(0);

        for (Team t : teams) {
            double teamAvg = t.members.stream().mapToInt(m -> m.skill).average().orElse(0);
            if (teamAvg > avg + 2) Collections.shuffle(t.members);
        }
    }

    public static void saveTeams(Path path, List<Team> teams) throws IOException {
        List<String> lines = new ArrayList<>();
        lines.add("TeamID,PlayerID,Name,Game,Skill,Role,PersonalityType");

        for (Team t : teams) {
            // List all members of this team
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

            // Add clean summary row
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
                        "Average Skill: " + String.format("%.1f", avgSkill),
                        "Unique Roles: " + uniqueRoles,
                        "Personalities: " + leaders + "L/" + balanced + "B/" + thinkers + "T",
                        ""
                ));
            }

            // Add blank line between teams
            lines.add("");
        }

        Files.createDirectories(path.getParent());
        Files.write(path, lines, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
    }
}