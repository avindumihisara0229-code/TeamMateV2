package main.java;

import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

public class TeamBuilder {


    // 1. MAIN BUILD METHOD (PARALLEL VERSION)

    public static List<Team> build(List<Participant> players, int teamSize) {
        if (players.isEmpty()) throw new IllegalArgumentException("No players available.");

        // 1. Prepare Pools (Thread-safe lists not strictly needed if we sync access,
        // but we copy to new lists to avoid modifying the original 'players' passed in)
        List<Participant> leaders = filterAndShuffle(players, Participant.PersonalityType.LEADER);
        List<Participant> thinkers = filterAndShuffle(players, Participant.PersonalityType.THINKER);
        List<Participant> balanced = filterAndShuffle(players, Participant.PersonalityType.BALANCED);

        // Calculate theoretical max teams based on Leader count
        int maxTeams = leaders.size();

        // 2. Setup Thread Pool (4 Workers)
        ExecutorService executor = Executors.newFixedThreadPool(4);
        List<Callable<Team>> tasks = new ArrayList<>();

        // 3. Create Tasks: One task per potential team
        for (int i = 0; i < maxTeams; i++) {
            final int teamId = i + 1;
            tasks.add(() -> buildSingleTeam(teamId, teamSize, leaders, thinkers, balanced));
        }

        List<Team> teams = new ArrayList<>();

        try {
            // 4. Run all tasks in parallel
            // invokeAll blocks until all tasks are done (or failed)
            List<Future<Team>> results = executor.invokeAll(tasks);

            // 5. Collect Results
            for (Future<Team> future : results) {
                try {
                    Team t = future.get(); // Retrieve result
                    if (t != null) {
                        teams.add(t);
                    }
                } catch (ExecutionException e) {
                    System.err.println("Error building a specific team: " + e.getMessage());
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            executor.shutdown(); // Stop the thread pool
        }

        // 6. Skill Balance (Sequential Post-Processing)
        // Balancing requires global knowledge of all teams, so we do this sequentially.
        balanceSkillLevels(teams);

        return teams;
    }


    // 2. THE WORKER TASK (Builds ONE Team)

    private static Team buildSingleTeam(int id, int teamSize,
                                        List<Participant> leaders,
                                        List<Participant> thinkers,
                                        List<Participant> balanced) {

        Team potentialTeam = new Team(id);
        List<Participant> selectedMembers = new ArrayList<>();

        // --- A. GET LEADER (Synchronized Lock) ---
        // We lock the 'leaders' list so only ONE thread can remove a player at a time
        synchronized (leaders) {
            Participant leader = findCompatiblePlayer(leaders, selectedMembers);
            if (leader == null) return null; // Logic break: No valid leader
            selectedMembers.add(leader);
            leaders.remove(leader);
        }

        // --- B. GET THINKERS (Synchronized Lock) ---
        int thinkersTarget = (new Random().nextBoolean()) ? 2 : 1;

        synchronized (thinkers) {
            if (thinkers.isEmpty()) return null;

            int thinkersAdded = 0;
            for (int i = 0; i < thinkersTarget; i++) {
                Participant thinker = findCompatiblePlayer(thinkers, selectedMembers);
                if (thinker != null) {
                    selectedMembers.add(thinker);
                    thinkers.remove(thinker);
                    thinkersAdded++;
                }
            }
            if (thinkersAdded < 1) return null; // Strict rule violation
        }

        // --- C. GET BALANCED (Synchronized Lock) ---
        synchronized (balanced) {
            int spotsRemaining = teamSize - selectedMembers.size();
            for (int i = 0; i < spotsRemaining; i++) {
                Participant bal = findCompatiblePlayer(balanced, selectedMembers);
                if (bal != null) {
                    selectedMembers.add(bal);
                    balanced.remove(bal);
                }
            }
        }

        // --- D. LOCAL VALIDATION ---
        potentialTeam.members.addAll(selectedMembers);

        boolean isFull = potentialTeam.members.size() == teamSize;
        boolean hasRoleVariety = potentialTeam.getUniqueRoleCount() >= 3;
        boolean validGameCap = checkGameCap(potentialTeam);

        long lCount = potentialTeam.members.stream().filter(m -> m.type == Participant.PersonalityType.LEADER).count();
        long tCount = potentialTeam.members.stream().filter(m -> m.type == Participant.PersonalityType.THINKER).count();

        // If valid, return the Team. If invalid, return null (Team discarded).
        if (isFull && hasRoleVariety && validGameCap && lCount == 1 && tCount >= 1 && tCount <= 2) {
            return potentialTeam;
        } else {
            return null;
        }
    }


    // 3. HELPERS

    private static Participant findCompatiblePlayer(List<Participant> pool, List<Participant> currentTeam) {
        if (pool.isEmpty()) return null;
        Map<String, Long> gameCounts = currentTeam.stream()
                .collect(Collectors.groupingBy(p -> p.game, Collectors.counting()));

        for (Participant candidate : pool) {
            long currentCount = gameCounts.getOrDefault(candidate.game, 0L);
            if (currentCount < 2) return candidate;
        }
        return null;
    }

    private static boolean checkGameCap(Team t) {
        Map<String, Long> counts = t.members.stream().collect(Collectors.groupingBy(p -> p.game, Collectors.counting()));
        return counts.values().stream().noneMatch(count -> count > 2);
    }

    private static List<Participant> filterAndShuffle(List<Participant> all, Participant.PersonalityType type) {
        List<Participant> filtered = all.stream()
                .filter(p -> p.type == type)
                .collect(Collectors.toList());
        Collections.shuffle(filtered);
        return new ArrayList<>(filtered); // Return ArrayList for simpler synchronization
    }


    // 4. SKILL BALANCING

    private static void balanceSkillLevels(List<Team> teams) {
        if (teams.size() < 2) return;
        int maxIterations = 500;
        boolean improvementMade = true;

        for (int k = 0; k < maxIterations && improvementMade; k++) {
            improvementMade = false;
            teams.sort(Comparator.comparingDouble(Team::getAverageSkill));
            Team weakest = teams.get(0);
            Team strongest = teams.get(teams.size() - 1);

            if ((strongest.getAverageSkill() - weakest.getAverageSkill()) < 1.0) break;

            if (trySwap(strongest, weakest)) improvementMade = true;
        }
    }

    private static boolean trySwap(Team highTeam, Team lowTeam) {
        for (Participant strongP : highTeam.members) {
            for (Participant weakP : lowTeam.members) {
                if (strongP.type != weakP.type) continue;
                if (strongP.skill <= weakP.skill) continue;
                if (isValidSwap(highTeam, strongP, weakP) && isValidSwap(lowTeam, weakP, strongP)) {
                    highTeam.members.remove(strongP);
                    highTeam.members.add(weakP);
                    lowTeam.members.remove(weakP);
                    lowTeam.members.add(strongP);
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean isValidSwap(Team team, Participant out, Participant in) {
        List<Participant> tempMembers = new ArrayList<>(team.members);
        tempMembers.remove(out);
        tempMembers.add(in);

        Map<String, Long> gameCounts = tempMembers.stream()
                .collect(Collectors.groupingBy(p -> p.game, Collectors.counting()));
        if (gameCounts.values().stream().anyMatch(c -> c > 2)) return false;

        long uniqueRoles = tempMembers.stream().map(p -> p.role).distinct().count();
        if (uniqueRoles < 3) return false;

        return true;
    }
}