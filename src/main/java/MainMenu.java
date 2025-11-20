package main.java;

import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.regex.Pattern;

public class MainMenu {

    // Regex for simple email validation
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9+_.-]+@(.+)$");

    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        Path defaultParticipants = Paths.get("data/participants.csv");
        Path defaultTeams = Paths.get("data/formed_teams.csv");

        while (true) {
            System.out.println("\n==== TEAM BUILDER MENU ====");
            System.out.println("1. Add Member + Survey");
            System.out.println("2. Form Teams (Default)");
            System.out.println("3. Import Participants (External CSV)");
            System.out.println("4. Export Formed Teams (Save As...)");
            System.out.println("5. Exit");
            System.out.print("Choose option: ");

            String choice = sc.nextLine().trim();

            if (choice.isEmpty()) {
                System.out.println("⚠️ Input cannot be empty.");
                continue;
            }

            try {
                switch (choice) {
                    case "1" -> addParticipant(sc, defaultParticipants);
                    case "2" -> formTeams(sc, defaultParticipants, defaultTeams);
                    case "3" -> importParticipants(sc, defaultParticipants);
                    case "4" -> exportTeams(sc, defaultTeams);
                    case "5" -> {
                        System.out.println("👋 Goodbye!");
                        return;
                    }
                    default -> System.out.println("⚠️ Invalid option. Please select 1-5.");
                }
            } catch (Exception e) {
                System.err.println("❌ Error: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    // 📥 Import external CSV
    private static void importParticipants(Scanner sc, Path defaultPath) {
        System.out.print("Enter full file path to import (e.g., C:/Downloads/class_list.csv): ");
        String inputPath = sc.nextLine().trim();
        Path source = Paths.get(inputPath);

        try {
            List<Participant> imported = CSVHandler.load(source);
            if (imported.isEmpty()) {
                System.out.println("⚠️ No valid participants found in that file.");
                return;
            }
            CSVHandler.save(defaultPath, imported);
            System.out.println("✅ Successfully imported " + imported.size() + " participants to system!");
        } catch (IOException e) {
            System.out.println("❌ File error: " + e.getMessage());
        }
    }

    // 📤 Export formed teams
    private static void exportTeams(Scanner sc, Path defaultTeamPath) {
        if (!Files.exists(defaultTeamPath)) {
            System.out.println("⚠️ No formed teams found. Please run 'Form Teams' (Option 2) first.");
            return;
        }

        System.out.print("Enter destination path (e.g., D:/Results/final_teams.csv): ");
        String destPath = sc.nextLine().trim();
        Path destination = Paths.get(destPath);

        try {
            CSVHandler.exportFile(defaultTeamPath, destination);
            System.out.println("✅ Teams exported successfully to: " + destination.toAbsolutePath());
        } catch (IOException e) {
            System.out.println("❌ Export failed: " + e.getMessage());
        }
    }

    private static void addParticipant(Scanner sc, Path path) throws IOException {
        List<Participant> participants = CSVHandler.load(path);

        System.out.print("Enter Player ID (e.g., P101): ");
        String id = getValidInput(sc, "ID");

        System.out.print("Enter Name: ");
        String name = getValidInput(sc, "Name");

        // 📧 FIXED: Uses helper method so 'email' is assigned only ONCE (Effectively Final)
        String email = getValidEmail(sc);

        System.out.println("\nSelect Game:");
        System.out.println("1. CS:GO\n2. Valorant\n3. Chess\n4. FIFA\n5. Dota 2");
        int gChoice = getValidInt(sc, 1, 5);
        String game = switch (gChoice) {
            case 1 -> "CS:GO";
            case 2 -> "Valorant";
            case 3 -> "Chess";
            case 4 -> "FIFA";
            case 5 -> "Dota 2";
            default -> "Unknown";
        };

        System.out.print("Enter Skill Level (1–10): ");
        int skill = getValidInt(sc, 1, 10);

        System.out.println("\nSelect Role:");
        System.out.println("1. Strategist\n2. Attacker\n3. Defender\n4. Supporter\n5. Coordinator");
        int rChoice = getValidInt(sc, 1, 5);
        Participant.Role role = Participant.Role.fromChoice(rChoice);

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
            answers[i] = getValidInt(sc, 1, 5);
        }

        // 🧵 Threading for Survey
        ExecutorService surveyThread = Executors.newSingleThreadExecutor();
        Future<Participant> future = surveyThread.submit(() -> {
            // 'email' is now safe to use because it is effectively final
            int score = Personality.calculateScore(answers);
            Participant.PersonalityType type = Personality.classify(score);
            return new Participant(id, name, email, game, skill, role, score, type);
        });

        Participant newP;
        try {
            newP = future.get();
        } catch (ExecutionException | InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Error in survey processing.");
        } finally {
            surveyThread.shutdown();
        }

        participants.add(newP);
        CSVHandler.save(path, participants);
        System.out.println("\n✅ Player added!");
    }

    private static void formTeams(Scanner sc, Path participantsPath, Path teamsPath)
            throws IOException, InterruptedException {
        List<Participant> players = CSVHandler.load(participantsPath);
        if (players.isEmpty()) {
            System.out.println("⚠️ No participants found. Add members or Import CSV (Option 3) first!");
            return;
        }

        System.out.print("Enter team size: ");
        int teamSize = getValidInt(sc, 1, players.size());

        System.out.println("\n⚙️ Forming balanced teams....");
        List<Team> teams = TeamBuilder.build(new ArrayList<>(players), teamSize);

        TeamBuilder.saveTeams(teamsPath, teams);
        System.out.println("\n✅ Teams saved to formed_teams.csv!");

        for (Team t : teams) {
            System.out.println(t.getSummary());
        }
    }

    // 🛠️ NEW HELPER: Logic extracted here to keep 'email' variable final in main method
    private static String getValidEmail(Scanner sc) {
        while (true) {
            System.out.print("Enter Email: ");
            String input = sc.nextLine().trim();
            if (EMAIL_PATTERN.matcher(input).matches()) {
                return input;
            }
            System.out.println("⚠️ Invalid email format! (e.g., user@example.com)");
        }
    }

    // 🛠️ HELPER: Validate Integers
    private static int getValidInt(Scanner sc, int min, int max) {
        int input;
        while (true) {
            try {
                String line = sc.nextLine();
                input = Integer.parseInt(line.trim());
                if (input >= min && input <= max) {
                    return input;
                } else {
                    System.out.printf("⚠️ Please enter a number between %d and %d: ", min, max);
                }
            } catch (NumberFormatException e) {
                System.out.print("⚠️ Invalid input. Enter a number: ");
            }
        }
    }

    // 🛠️ HELPER: Validate String Input
    private static String getValidInput(Scanner sc, String fieldName) {
        String input;
        while (true) {
            input = sc.nextLine().trim();
            if (!input.isEmpty()) return input;
            System.out.printf("⚠️ %s cannot be empty. Try again: ", fieldName);
        }
    }
}