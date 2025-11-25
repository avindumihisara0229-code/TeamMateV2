package main.java;

import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.regex.Pattern;

public class MainMenu {

    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9+_.-]+@(.+)$");

    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        Path defaultParticipants = Paths.get("data/participants.csv");
        Path defaultTeams = Paths.get("data/formed_teams.csv");

        while (true) {
            System.out.println("\n==== TEAM BUILDER MENU ====");
            System.out.println("1. Add Member + Survey");
            System.out.println("2. Form Teams (Default)");
            System.out.println("3. View Last Formed Teams");
            System.out.println("4. Upload CSV (External CSV)");
            System.out.println("5. Exit");
            System.out.print("Choose option: ");

            String choice = sc.nextLine().trim();

            if (choice.isEmpty()) {
                System.out.println("Input cannot be empty.");
                continue;
            }

            try {
                switch (choice) {
                    case "1" -> addParticipant(sc, defaultParticipants);
                    case "2" -> formTeams(sc, defaultParticipants, defaultTeams);
                    case "3" -> viewTeams(defaultTeams); // Uses new table formatter
                    case "4" -> importParticipants(sc, defaultParticipants);
                    case "5" -> {
                        System.out.println("Goodbye!");
                        return;
                    }
                    default -> System.out.println("Invalid option. Please select 1-5.");
                }
            } catch (Exception e) {
                System.err.println("Error: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }


    // NEW METHOD: View Teams (Table Format)

    private static void viewTeams(Path teamsPath) {
        System.out.println("\n--- EXISTING TEAMS DATA ---");
        try {
            List<String> lines = CSVHandler.loadRaw(teamsPath);

            if (lines.isEmpty()) {
                System.out.println("No team data found. Please form teams first.");
                return;
            }

            // 1. Read all data to calculate column widths
            List<String[]> rows = new ArrayList<>();
            // We expect 7 columns based on CSVHandler.saveTeams
            int[] colWidths = new int[7];

            for (String line : lines) {
                if (line.trim().isEmpty()) {
                    rows.add(null); // Add null to represent a separator line (empty space in CSV)
                    continue;
                }

                // Split by comma, -1 ensures we keep empty trailing columns
                String[] cells = line.split(",", -1);
                rows.add(cells);

                // Update max width for each column
                for (int i = 0; i < cells.length && i < colWidths.length; i++) {
                    int length = cells[i].trim().length();
                    if (length > colWidths[i]) {
                        colWidths[i] = length;
                    }
                }
            }

            // 2. Build the format string (e.g., "| %-10s | %-15s | ...")
            StringBuilder formatBuilder = new StringBuilder();
            for (int width : colWidths) {
                formatBuilder.append("| %-").append(width).append("s ");
            }
            formatBuilder.append("|%n"); // Newline
            String format = formatBuilder.toString();

            // 3. Print the Table
            printTableSeparator(colWidths);

            for (String[] row : rows) {
                if (row == null) {
                    // Print a separator line when we hit a null (empty line in CSV)
                    printTableSeparator(colWidths);
                } else {
                    // Create an object array padded to 7 columns to avoid crashes
                    Object[] args = new Object[7];
                    for (int i = 0; i < 7; i++) {
                        args[i] = (i < row.length) ? row[i].trim() : "";
                    }
                    System.out.printf(format, args);
                }
            }
            printTableSeparator(colWidths);

        } catch (IOException e) {
            System.out.println("Error reading file: " + e.getMessage());
        }
    }

    // Helper to print the horizontal lines (+--------+-------+)
    private static void printTableSeparator(int[] widths) {
        System.out.print("+");
        for (int w : widths) {
            // w + 2 accounts for the extra spaces added in the format string
            System.out.print("-".repeat(w + 2) + "+");
        }
        System.out.println();
    }


    // Existing Methods (Unchanged)


    private static void importParticipants(Scanner sc, Path defaultPath) {
        System.out.print("Enter full file path to import (e.g., C:/Downloads/class_list.csv): ");
        String inputPath = sc.nextLine().trim();
        Path source = Paths.get(inputPath);

        try {
            List<Participant> imported = CSVHandler.load(source);
            if (imported.isEmpty()) {
                System.out.println("No valid participants found in that file.");
                return;
            }
            CSVHandler.save(defaultPath, imported);
            System.out.println("Successfully imported " + imported.size() + " participants to system!");
        } catch (IOException e) {
            System.out.println("File error: " + e.getMessage());
        }
    }

    private static void addParticipant(Scanner sc, Path path) throws IOException {
        List<Participant> participants = CSVHandler.load(path);

        System.out.print("Enter Player ID (e.g., P101): ");
        String id = getValidInput(sc, "ID");

        System.out.print("Enter Name: ");
        String name = getValidInput(sc, "Name");

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

        System.out.print("Enter Skill Level (1-10): ");
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
            System.out.print("Rate (1-5): ");
            answers[i] = getValidInt(sc, 1, 5);
        }

        ExecutorService surveyThread = Executors.newSingleThreadExecutor();
        Future<Participant> future = surveyThread.submit(() -> {
            int score = Personality.calculateScore(answers);
            Participant.PersonalityType type = Personality.classify(score);
            return new Participant(id, name, email, game, skill, role, score, type);
        });

        Participant newP;
        try {
            newP = future.get();
        } catch (Exception e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Error in survey processing.");
        } finally {
            surveyThread.shutdown();
        }

        participants.add(newP);
        CSVHandler.save(path, participants);
        System.out.println("\nPlayer added!");
    }

    private static void formTeams(Scanner sc, Path participantsPath, Path teamsPath)
            throws IOException, InterruptedException {

        List<Participant> players = CSVHandler.load(participantsPath);
        if (players.isEmpty()) {
            System.out.println("No participants found. Add members or Import CSV first!");
            return;
        }

        System.out.print("Enter team size: ");
        int teamSize = getValidInt(sc, 1, players.size());

        System.out.print("\nForming balanced teams (Parallel Processing)");

        ExecutorService uiExecutor = Executors.newSingleThreadExecutor();

        Future<List<Team>> futureTeams = uiExecutor.submit(() ->
                TeamBuilder.build(new ArrayList<>(players), teamSize)
        );

        while (!futureTeams.isDone()) {
            System.out.print(".");
            Thread.sleep(300);
        }

        System.out.println(" Done!");

        List<Team> teams;
        try {
            teams = futureTeams.get();
        } catch (ExecutionException e) {
            System.err.println("\nError: " + e.getMessage());
            uiExecutor.shutdown();
            return;
        } finally {
            uiExecutor.shutdown();
        }

        CSVHandler.saveTeams(teamsPath, teams);
        System.out.println("Teams saved!");

        // Removed the text user can now use Option 3 to view it nicely
        System.out.println("Use Option 3 to view the generated teams.");
    }

    private static String getValidEmail(Scanner sc) {
        while (true) {
            System.out.print("Enter Email: ");
            String input = sc.nextLine().trim();
            if (EMAIL_PATTERN.matcher(input).matches()) {
                return input;
            }
            System.out.println("Invalid email format!");
        }
    }

    private static int getValidInt(Scanner sc, int min, int max) {
        while (true) {
            try {
                int input = Integer.parseInt(sc.nextLine().trim());
                if (input >= min && input <= max) {
                    return input;
                }
                System.out.printf("Enter a number between %d and %d: ", min, max);
            } catch (NumberFormatException e) {
                System.out.print("Invalid number. Try again: ");
            }
        }
    }

    private static String getValidInput(Scanner sc, String fieldName) {
        while (true) {
            String input = sc.nextLine().trim();
            if (!input.isEmpty()) return input;
            System.out.printf("%s cannot be empty. Try again: ", fieldName);
        }
    }
}