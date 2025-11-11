package main.java;

import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;

public class MainMenu {

    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        Path participantsPath = Paths.get("data/participants.csv");
        Path teamsPath = Paths.get("data/formed_teams.csv");

        while (true) {
            System.out.println("\n==== TEAM BUILDER MENU ====");
            System.out.println("1. Add Member + Survey");
            System.out.println("2. Form Teams");
            System.out.println("3. Exit");
            System.out.print("Choose option: ");
            String choice = sc.nextLine();

            try {
                switch (choice) {
                    case "1" -> addParticipant(sc, participantsPath);
                    case "2" -> formTeams(sc, participantsPath, teamsPath);
                    case "3" -> {
                        System.out.println("👋 Goodbye!");
                        return;
                    }
                    default -> System.out.println("Invalid option.");
                }
            } catch (Exception e) {
                System.err.println("❌ Error: " + e.getMessage());
            }
        }
    }

    private static void addParticipant(Scanner sc, Path path) throws IOException, InterruptedException {
        List<Participant> participants = CSVHandler.load(path);

        System.out.print("Enter Player ID (e.g., P101): ");
        String id = sc.nextLine().trim();
        System.out.print("Enter Name: ");
        String name = sc.nextLine().trim();
        System.out.print("Enter Email: ");
        String email = sc.nextLine().trim();

        System.out.println("\nSelect Game:");
        System.out.println("1. CS:GO\n2. Valorant\n3. Chess\n4. FIFA\n5. Dota 2");
        int gChoice = Integer.parseInt(sc.nextLine());
        String game = switch (gChoice) {
            case 1 -> "CS:GO";
            case 2 -> "Valorant";
            case 3 -> "Chess";
            case 4 -> "FIFA";
            case 5 -> "Dota 2";
            default -> "Unknown";
        };

        System.out.print("Enter Skill Level (1–10): ");
        int skill = Integer.parseInt(sc.nextLine());

        System.out.println("\nSelect Role:");
        System.out.println("1. Strategist\n2. Attacker\n3. Defender\n4. Supporter\n5. Coordinator");
        int rChoice = Integer.parseInt(sc.nextLine());
        Participant.Role role = Participant.Role.fromChoice(rChoice);
        if (role == Participant.Role.UNKNOWN)
            throw new IllegalArgumentException("Invalid role selected!");

        System.out.println("\n--- Personality Survey ---");
        String[] questions = {
                "I enjoy leading groups.",
                "I stay calm under pressure.",
                "I prefer analyzing before acting.",
                "I enjoy supporting teammates.",
                "I like communicating and coordinating."
        };

        int[] answers = new int[5];
        for (int i = 0; i < questions.length; i++) {
            System.out.println(questions[i]);
            System.out.print("Rate (1–5): ");
            answers[i] = Integer.parseInt(sc.nextLine());
        }

        // ⚙️ Process survey in a separate thread
        ExecutorService surveyThread = Executors.newSingleThreadExecutor();
        Future<Participant> future = surveyThread.submit(() -> {
            int score = Personality.calculateScore(answers);
            Participant.PersonalityType type = Personality.classify(score);
            return new Participant(id, name, email, game, skill, role, score, type);
        });

        Participant newP = null;
        try {
            newP = future.get(); // waits for survey scoring
        } catch (Exception e) {
            throw new RuntimeException("Error calculating personality: " + e.getMessage());
        }
        surveyThread.shutdown();

        // Add to CSV
        participants.add(newP);
        CSVHandler.save(path, participants);

        // ✅ Preview in console
        System.out.println("\n✅ Player added successfully!");
        System.out.println("---------------------------------------");
        System.out.printf("ID: %s%nName: %s%nGame: %s%nSkill: %d%nRole: %s%nPersonality: %s (%d)%n",
                newP.id, newP.name, newP.game, newP.skill, newP.role, newP.type, newP.score);
        System.out.println("---------------------------------------");
    }

    private static void formTeams(Scanner sc, Path participantsPath, Path teamsPath)
            throws IOException, InterruptedException {
        List<Participant> players = CSVHandler.load(participantsPath);
        if (players.isEmpty()) {
            System.out.println("No participants found. Add members first!");
            return;
        }

        System.out.print("Enter team size: ");
        int teamSize = Integer.parseInt(sc.nextLine());

        System.out.println("⚙️ Forming balanced teams...");
        List<Team> teams = TeamBuilder.build(new ArrayList<>(players), teamSize);

        TeamBuilder.saveTeams(teamsPath, teams);
        System.out.println("✅ Teams created and saved to formed_teams.csv!");
    }
}
