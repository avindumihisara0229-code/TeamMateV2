package main.java;

import java.util.ArrayList;
import java.util.List;

public class Team {
    int id;
    List<Participant> members = new ArrayList<>();

    public Team(int id) {
        this.id = id;
    }

    public double getAverageSkill() {
        return members.stream().mapToInt(m -> m.skill).average().orElse(0);
    }

    public int getUniqueRoleCount() {
        return (int) members.stream().map(m -> m.role).distinct().count();
    }

    public String getSummary() {
        long leaders = members.stream().filter(m -> m.type == Participant.PersonalityType.LEADER).count();
        long thinkers = members.stream().filter(m -> m.type == Participant.PersonalityType.THINKER).count();
        long balanced = members.stream().filter(m -> m.type == Participant.PersonalityType.BALANCED).count();

        return String.format("Team %d → Avg Skill: %.1f | Roles: %d | Personality: %dL/%dB/%dT",
                id, getAverageSkill(), getUniqueRoleCount(), leaders, balanced, thinkers);
    }
}
