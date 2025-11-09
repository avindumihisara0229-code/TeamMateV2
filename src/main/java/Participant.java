package main.java;

public class Participant {
    String id, name, email, game;
    Role role;
    int skill, score;
    PersonalityType type;

    public Participant(String id, String name, String email, String game, int skill, Role role, int score, PersonalityType type) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.game = game;
        this.skill = skill;
        this.role = role;
        this.score = score;
        this.type = type;
    }

    public enum Role {
        STRATEGIST, ATTACKER, DEFENDER, SUPPORTER, COORDINATOR, UNKNOWN;

        public static Role fromChoice(int c) {
            return switch (c) {
                case 1 -> STRATEGIST;
                case 2 -> ATTACKER;
                case 3 -> DEFENDER;
                case 4 -> SUPPORTER;
                case 5 -> COORDINATOR;
                default -> UNKNOWN;
            };
        }
    }

    public enum PersonalityType { LEADER, BALANCED, THINKER }
}
