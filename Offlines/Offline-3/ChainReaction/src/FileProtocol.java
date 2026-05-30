import java.io.*;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;

public class FileProtocol {
    private static final String FILENAME = "gamestate.txt";
    private static final String TEMP_FILENAME = "gamestate.tmp";
    private static final Object fileLock = new Object();

    public static void writeGameState(GameState state) {
        synchronized (fileLock) {
            try {
                StringBuilder content = new StringBuilder();

                // Header
                if (state.getCurrentPlayer() != null) {
                    content.append(state.getCurrentPlayer().getName()).append(" Move:\n");
                }

                // Board state
                content.append(state.toString()).append("\n");

                System.out.println("Writing game state for player: " +
                        (state.getCurrentPlayer() != null ? state.getCurrentPlayer().getColor() : "UNKNOWN"));

                // Write to temporary file first, then rename for atomic operation
                Files.write(Paths.get(TEMP_FILENAME), content.toString().getBytes());
                Files.move(Paths.get(TEMP_FILENAME), Paths.get(FILENAME), StandardCopyOption.REPLACE_EXISTING);

            } catch (IOException e) {
                System.err.println("Error writing game state: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    public static GameState readGameState() {
        synchronized (fileLock) {
            try {
                if (!fileExists()) {
                    return null;
                }

                // Check if file is being written to (very basic check)
                Path filePath = Paths.get(FILENAME);
                BasicFileAttributes attrs = Files.readAttributes(filePath, BasicFileAttributes.class);

                // If file is too new, wait a bit
                if (System.currentTimeMillis() - attrs.lastModifiedTime().toMillis() < 100) {
                    Thread.sleep(150);
                }

                String content = new String(Files.readAllBytes(filePath)).trim();

                if (content.isEmpty()) {
                    return null;
                }

                String[] lines = content.split("\n");

                if (lines.length < 2) {
                    System.err.println("Invalid file format - not enough lines");
                    return null;
                }

                GameState state = new GameState();

                // Parse header to determine current player
                String header = lines[0].trim();
                System.out.println("Reading game state with header: " + header);

                if (header.contains("Human") || header.contains("RED") || header.contains("Red")) {
                    // If header mentions human/red, next turn should be AI/Blue
                    state.setCurrentPlayer(state.getPlayers().get(1));
                } else if (header.contains("AI") || header.contains("BLUE") || header.contains("Blue")) {
                    // If header mentions AI/blue, next turn should be Human/Red
                    state.setCurrentPlayer(state.getPlayers().get(0));
                } else {
                    // Default fallback
                    state.setCurrentPlayer(state.getPlayers().get(0));
                }

                // Parse board state
                for (int i = 1; i < lines.length && i <= state.NumRow; i++) {
                    String line = lines[i].trim();
                    if (line.isEmpty()) continue;

                    String[] cells = line.split(" ");
                    for (int j = 0; j < cells.length && j < state.NumColumn; j++) {
                        String cellStr = cells[j].trim();

                        int orbCount = 0;
                        COLOR color = COLOR.WHITE;

                        if (!cellStr.equals("0") && !cellStr.isEmpty()) {
                            try {
                                if (cellStr.length() > 1) {
                                    // Extract number and color
                                    String numberPart = cellStr.substring(0, cellStr.length() - 1);
                                    char colorChar = cellStr.charAt(cellStr.length() - 1);

                                    orbCount = Integer.parseInt(numberPart);
                                    color = switch (colorChar) {
                                        case 'R' -> COLOR.RED;
                                        case 'B' -> COLOR.BLUE;
                                        default -> COLOR.WHITE;
                                    };
                                } else if (cellStr.matches("\\d+")) {
                                    // Just a number, treat as count with no color
                                    orbCount = Integer.parseInt(cellStr);
                                    color = COLOR.WHITE;
                                }
                            } catch (NumberFormatException | StringIndexOutOfBoundsException e) {
                                System.err.println("Error parsing cell: " + cellStr + " - " + e.getMessage());
                                orbCount = 0;
                                color = COLOR.WHITE;
                            }
                        }

                        state.board[i-1][j] = new Cell(orbCount, color, i - 1, j, state.NumRow, state.NumColumn);
                    }
                }

                return state;

            } catch (IOException e) {
                System.err.println("Error reading game state: " + e.getMessage());
                return null;
            } catch (InterruptedException e) {
                System.err.println("Interrupted while reading game state");
                return null;
            } catch (Exception e) {
                System.err.println("Unexpected error reading game state: " + e.getMessage());
                e.printStackTrace();
                return null;
            }
        }
    }

    public static boolean fileExists() {
        return Files.exists(Paths.get(FILENAME));
    }

    public static void deleteGameStateFile() {
        synchronized (fileLock) {
            try {
                Files.deleteIfExists(Paths.get(FILENAME));
                Files.deleteIfExists(Paths.get(TEMP_FILENAME));
            } catch (IOException e) {
                System.err.println("Error deleting game state files: " + e.getMessage());
            }
        }
    }
}