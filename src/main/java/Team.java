package main.java;

import java.util.ArrayList;
import java.util.List;

public class Team {
    int id;
    List<Participant> members = new ArrayList<>();

    public Team(int id) {
        this.id = id;
    }

    // Calculate the average skill level of the team
    public double getAverageSkill() {
        return members.stream()
                .mapToInt(m -> m.skill)
                .average()
                .orElse(0); // Handle empty team case
    }

    // Count how many unique roles exist in the team
    public int getUniqueRoleCount() {
        return (int) members.stream()
                .map(m -> m.role)
                .distinct()
                .count();
    }

    // Generate a text summary of the team stats for the console
    public String getSummary() {
        // Calculate breakdown of personality types
        long leaders = members.stream().filter(m -> m.type == Participant.PersonalityType.LEADER).count();
        long thinkers = members.stream().filter(m -> m.type == Participant.PersonalityType.THINKER).count();
        long balanced = members.stream().filter(m -> m.type == Participant.PersonalityType.BALANCED).count();

        // Format: Team ID -> Avg Skill | Unique Roles | Personality Breakdown (L/B/T)
        return String.format("Team %d → Avg Skill: %.1f | Roles: %d | Personality: %dL/%dB/%dT",
                id, getAverageSkill(), getUniqueRoleCount(), leaders, balanced, thinkers);
    }
}