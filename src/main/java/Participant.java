package main.java;

public class Participant {
    // Core participant attributes
    String id, name, email, game;
    Role role;
    int skill, score;
    PersonalityType type;

    //  Constructor to initialize all fields
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

    //  Enum defining specific gameplay roles
    public enum Role {
        STRATEGIST, ATTACKER, DEFENDER, SUPPORTER, COORDINATOR, UNKNOWN;

        // Helper method to convert menu input (int) to Enum
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

    //  Enum for personality classification derived from survey
    public enum PersonalityType { LEADER, BALANCED, THINKER }
}