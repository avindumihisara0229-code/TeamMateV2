package main.java;

import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;

public class MainMenu {

    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        // file paths for data
        Path participantsPath = Paths.get("data/participants.csv");
        Path teamsPath = Paths.get("data/formed_teams.csv");

        // Main application
        while (true) {
            System.out.println("\n==== TEAM BUILDER MENU ====");
            System.out.println("1. Add Member + Survey");
            System.out.println("2. Form Teams");
            System.out.println("3. Exit");
            System.out.print("Choose option: ");
            String choice = sc.nextLine();

            try {
                // menu selection
                switch (choice) {
                    case "1" -> addParticipant(sc, participantsPath);
                    case "2" -> formTeams(sc, participantsPath, teamsPath);
                    case "3" -> {
                        System.out.println("👋 Goodbye!");
                        return; // Break the while loop and exit
                    }
                    default -> System.out.println("Invalid option.");
                }
            } catch (Exception e) {
                // Global error handler for the menu loop
                System.err.println("❌ Error: " + e.getMessage());
            }
        }
    }

    //  add new participant + personality survey
    private static void addParticipant(Scanner sc, Path path) throws IOException {
        // Load existing participants to append to the list later
        List<Participant> participants = CSVHandler.load(path);

        // --- Basic Information ---
        System.out.print("Enter Player ID (e.g., P101): ");
        String id = sc.nextLine().trim();
        System.out.print("Enter Name: ");
        String name = sc.nextLine().trim();
        System.out.print("Enter Email: ");
        String email = sc.nextLine().trim();

        // --- Game Selection ---
        System.out.println("\nSelect Game:");
        System.out.println("1. CS:GO\n2. Valorant\n3. Chess\n4. FIFA\n5. Dota 2");
        int gChoice = Integer.parseInt(sc.nextLine());

        // Map integer input to specific game string
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

        // --- Role Selection ---
        System.out.println("\nSelect Role:");
        System.out.println("1. Strategist\n2. Attacker\n3. Defender\n4. Supporter\n5. Coordinator");
        int rChoice = Integer.parseInt(sc.nextLine());

        // Convert choice to Enum
        Participant.Role role = Participant.Role.fromChoice(rChoice);
        if (role == Participant.Role.UNKNOWN)
            throw new IllegalArgumentException("Invalid role selected!");

        // --- Personality Survey Input ---
        System.out.println("\n--- Personality Survey ---");
        String[] questions = {
                "I enjoy leading groups.",
                "I stay calm under pressure.",
                "I prefer analyzing before acting.",
                "I enjoy supporting teammates.",
                "I like communicating and coordinating."
        };

        int[] answers = new int[5];
        // Loop through questions with input validation
        for (int i = 0; i < questions.length; i++) {
            System.out.println(questions[i]);
            System.out.print("Rate (1–5): ");
            try {
                answers[i] = Integer.parseInt(sc.nextLine());
                // Ensure rating is within bounds
                if (answers[i] < 1 || answers[i] > 5) throw new NumberFormatException();
            } catch (NumberFormatException e) {
                System.out.println("Invalid input! Please enter a number between 1 and 5.");
                i--; // Decrement index to retry the current question
            }
        }

        // Run survey processing on a separate thread
        // Using a single thread executor for the background calculation
        ExecutorService surveyThread = Executors.newSingleThreadExecutor();
        Future<Participant> future = surveyThread.submit(() -> {
            // Calculate score and determine personality type
            int score = Personality.calculateScore(answers);
            Participant.PersonalityType type = Personality.classify(score);
            // Return the fully constructed Participant object
            return new Participant(id, name, email, game, skill, role, score, type);
        });

        Participant newP = null;
        try {
            newP = future.get(); // Block main thread and wait for the result
        } catch (ExecutionException e) {
            throw new RuntimeException("Error calculating personality: " + e.getCause());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt(); // Restore interrupt status
            throw new RuntimeException("Thread interrupted while processing survey.");
        } finally {
            surveyThread.shutdown(); // Always clean up the thread pool
        }

        // Add participant and save to CSV
        participants.add(newP);
        CSVHandler.save(path, participants);

        // Preview summary in console
        System.out.println("\n✅ Player added successfully!");
        System.out.println("---------------------------------------");
        System.out.printf("ID: %s%nName: %s%nEmail: %s%nGame: %s%nSkill: %d%nRole: %s%nPersonality: %s (%d)%n",
                newP.id, newP.name, newP.email, newP.game, newP.skill, newP.role, newP.type, newP.score);
        System.out.println("---------------------------------------");
    }

    // Form and save balanced teams
    private static void formTeams(Scanner sc, Path participantsPath, Path teamsPath)
            throws IOException, InterruptedException {
        // Load current pool of players
        List<Participant> players = CSVHandler.load(participantsPath);

        if (players.isEmpty()) {
            System.out.println("No participants found. Add members first!");
            return;
        }

        System.out.print("Enter team size: ");
        int teamSize = Integer.parseInt(sc.nextLine());

        System.out.println("\n⚙️ Forming balanced teams....");

        // Pass a copy of the list to the TeamBuilder logic
        List<Team> teams = TeamBuilder.build(new ArrayList<>(players), teamSize);

        // Save result to file
        TeamBuilder.saveTeams(teamsPath, teams);
        System.out.println("\n✅ Teams created and saved to formed_teams.csv!");

        // Display summary in console
        System.out.println("\n=== TEAM SUMMARIES ===");
        for (Team t : teams) {
            System.out.println(t.getSummary());
        }
    }
}