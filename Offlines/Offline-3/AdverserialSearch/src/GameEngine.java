
// GameEngine.java
import java.io.*;
import java.util.concurrent.TimeUnit;

public class GameEngine {
    private static final String GAME_FILE = "gamestate.txt";
    private static final char AI_PLAYER = GameState.BLUE;
    private static final int MAX_DEPTH = 4;

    private MinimaxAgent agent;

    public GameEngine() {
        agent = new MinimaxAgent(AI_PLAYER, MAX_DEPTH);
    }

    public void run() {
        System.out.println("Game Engine started. Waiting for human moves...");

        while (true) {
            try {
                // Wait for human move
                GameState state = waitForHumanMove();

                if (state.isGameOver()) {
                    System.out.println("Game Over! Winner: " + state.getWinner());
                    break;
                }

                // Make AI move
                long startTime = System.currentTimeMillis();
                int[] move = agent.findBestMove(state);
                long endTime = System.currentTimeMillis();

                System.out.println("AI thinking time: " + (endTime - startTime) + "ms");
                System.out.println("AI move: (" + move[0] + ", " + move[1] + ")");

                state.makeMove(move[0], move[1]);
                state.saveToFile(GAME_FILE, false);

                if (state.isGameOver()) {
                    System.out.println("Game Over! Winner: " + state.getWinner());
                    break;
                }

            } catch (IOException e) {
                System.err.println("Error reading/writing game file: " + e.getMessage());
                break;
            } catch (InterruptedException e) {
                System.err.println("Thread interrupted: " + e.getMessage());
                break;
            }
        }
    }

    private GameState waitForHumanMove() throws IOException, InterruptedException {
        while (true) {
            try {
                File file = new File(GAME_FILE);
                if (file.exists()) {
                    GameState state = GameState.loadFromFile(GAME_FILE);
                    // Check if it's a human move and AI's turn
                    if (state.getCurrentPlayer() == AI_PLAYER) {
                        return state;
                    }
                }
            } catch (IOException e) {
                // File might be being written, wait and try again
            }

            TimeUnit.MILLISECONDS.sleep(100);
        }
    }

    public static void main(String[] args) {
        GameEngine engine = new GameEngine();
        engine.run();
    }
}




// Compile and run instructions:
// 1. Save each class in separate .java files
// 2. Compile: javac *.java
// 3. Run UI: java ChainReactionUI
// 4. Run Game Engine in separate terminal: java GameEngine