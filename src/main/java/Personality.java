package main.java;

import java.util.Arrays;

public class Personality {

    // Calculate total score from survey answers
    public static int calculateScore(int[] answers) {
        // Sum all ratings (Input range: 5 to 25)
        int sum = Arrays.stream(answers).sum();
        return sum * 4; // Scale to a 20–100 score range for easier classification
    }

    // Determine personality type based on score thresholds
    public static Participant.PersonalityType classify(int score) {
        if (score >= 90) return Participant.PersonalityType.LEADER;
        if (score >= 70) return Participant.PersonalityType.BALANCED;
        return Participant.PersonalityType.THINKER; // Default for scores below 70
    }
}