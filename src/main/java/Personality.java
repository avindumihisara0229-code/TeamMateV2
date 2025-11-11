package main.java;

import java.util.Arrays;

public class Personality {
    public static int calculateScore(int[] answers) {
        int sum = Arrays.stream(answers).sum();
        return sum * 4; // Scale 20–100
    }

    public static Participant.PersonalityType classify(int score) {
        if (score >= 90) return Participant.PersonalityType.LEADER;
        if (score >= 70) return Participant.PersonalityType.BALANCED;
        return Participant.PersonalityType.THINKER;
    }
}
