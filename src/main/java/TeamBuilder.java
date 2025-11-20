package main.java;

import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;

public class TeamBuilder {

    // Main method to orchestrate the team generation process
    public static List<Team> build(List<Participant> players, int teamSize) throws InterruptedException {
        if (players.isEmpty()) throw new IllegalArgumentException("No players available.");

        // Randomize player list to ensure fairness before chunking
        Collections.shuffle(players);

        int totalTeams = (int) Math.ceil(players.size() / (double) teamSize);
        List<Team> teams = new ArrayList<>();
        for (int i = 0; i < totalTeams; i++) teams.add(new Team(i + 1));

        // Divide participants into rough "chunks" based on team size
        List<List<Participant>> chunks = new ArrayList<>();
        for (int i = 0; i < players.size(); i += teamSize) {
            chunks.add(players.subList(i, Math.min(i + teamSize, players.size())));
        }

        // Process team formation in parallel using a Thread Pool
        // Limits threads to 4 or totalTeams (whichever is smaller) to prevent overhead
        ExecutorService ex = Executors.newFixedThreadPool(Math.min(totalTeams, 4));

        for (int i = 0; i < chunks.size(); i++) {
            final int teamIndex = i;
            // Submit task to background thread
            ex.submit(() -> allocateBalancedTeam(chunks.get(teamIndex), teams.get(teamIndex)));
        }

        ex.shutdown();
        ex.awaitTermination(10, TimeUnit.SECONDS); // Wait for all teams to be formed

        //Post-processing check for balance
        balanceSkillLevels(teams);
        return teams;
    }

    // Logic to fill a specific team based on game and role constraints
    private static void allocateBalancedTeam(List<Participant> chunk, Team team) {
        Map<String, Long> gameCount = new HashMap<>();
        Set<Participant.Role> roles = new HashSet<>();
        List<Participant> finalMembers = new ArrayList<>();

        // Step A: Add players while respecting Game Caps (Max 2 per game)
        for (Participant p : chunk) {
            long count = gameCount.getOrDefault(p.game, 0L);
            if (count < 2) {
                finalMembers.add(p);
                gameCount.put(p.game, count + 1);
                roles.add(p.role);
            }
        }

        // Step B: If team is not full (due to caps), fill with remaining chunk members
        if (finalMembers.size() < chunk.size()) {
            for (Participant p : chunk) {
                if (!finalMembers.contains(p)) {
                    finalMembers.add(p);
                }
                if (finalMembers.size() >= chunk.size()) break;
            }
        }

        // Step C: Personality Check - Ensure at least one 'LEADER' type
        boolean hasLeader = finalMembers.stream().anyMatch(m -> m.type == Participant.PersonalityType.LEADER);
        if (!hasLeader) {
            // Find a leader in the original chunk and swap them in
            for (Participant p : chunk) {
                if (p.type == Participant.PersonalityType.LEADER && !finalMembers.contains(p)) {
                    finalMembers.set(finalMembers.size() - 1, p); // Swap last member
                    break;
                }
            }
        }

        team.members.addAll(finalMembers);
    }

    // Skill balancing check
    private static void balanceSkillLevels(List<Team> teams) {
        double avg = teams.stream()
                .mapToDouble(t -> t.members.stream().mapToInt(m -> m.skill).average().orElse(0))
                .average().orElse(0);

        // If a team is significantly overpowered (Avg + 2), re-shuffle their internal order
        // (Note: In a real scenario, this might require swapping players between teams)
        for (Team t : teams) {
            double teamAvg = t.members.stream().mapToInt(m -> m.skill).average().orElse(0);
            if (teamAvg > avg + 2) Collections.shuffle(t.members);
        }
    }

    // Save teams to CSV with specific formatting and summaries
    public static void saveTeams(Path path, List<Team> teams) throws IOException {
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
                        "Average Skill: " + String.format("%.1f", avgSkill),
                        "Unique Roles: " + uniqueRoles,
                        "Personalities: " + leaders + "L/" + balanced + "B/" + thinkers + "T",
                        ""
                ));
            }
            lines.add("");
        }

        Files.createDirectories(path.getParent());
        Files.write(path, lines, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
    }
}