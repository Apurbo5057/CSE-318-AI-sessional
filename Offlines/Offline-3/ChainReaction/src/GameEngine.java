public class GameEngine {
    private COLOR aiColor;
    private int maxDepth;
    private MiniMaxAgent agent;
    private String lastProcessedState = "";

    public GameEngine(COLOR aiColor, int maxDepth) {
        this.aiColor = aiColor;
        this.maxDepth = maxDepth;
        this.agent = new MiniMaxAgent(aiColor, maxDepth);
    }

    public static void main(String[] args) {
        if (args.length != 2) {
            System.err.println("Usage: java GameEngine <COLOR> <DEPTH>");
            System.err.println("COLOR: RED or BLUE");
            System.err.println("DEPTH: integer (recommended 3-5)");
            return;
        }

        COLOR aiColor;
        try {
            aiColor = COLOR.valueOf(args[0].toUpperCase());
        } catch (IllegalArgumentException e) {
            System.err.println("Invalid color. Use RED or BLUE");
            return;
        }

        int depth;
        try {
            depth = Integer.parseInt(args[1]);
            if (depth < 1 || depth > 10) {
                System.err.println("Depth should be between 1 and 10");
                return;
            }
        } catch (NumberFormatException e) {
            System.err.println("Invalid depth. Use integer value");
            return;
        }

        GameEngine engine = new GameEngine(aiColor, depth);
        engine.run();
    }

    private void run() {
        System.out.println("AI Engine started for color: " + aiColor + " with depth: " + maxDepth);

        int consecutiveErrors = 0;
        final int MAX_CONSECUTIVE_ERRORS = 10;

        while (true) {
            try {
                // Wait for file to exist
                int waitCount = 0;
                while (!FileProtocol.fileExists()) {
                    Thread.sleep(200);
                    waitCount++;
                    if (waitCount > 150) { // 30 seconds timeout
                        System.out.println("Timeout waiting for game file. Exiting...");
                        return;
                    }
                }

                // Read current game state
                GameState currentState = FileProtocol.readGameState();

                if (currentState == null) {
                    Thread.sleep(200);
                    continue;
                }

                // Check if game is over
                if (currentState.isGameOver()) {
                    System.out.println("Game over detected. AI " + aiColor + " exiting...");
                    break;
                }

                // Convert state to string for comparison
                String currentStateString = currentState.toString() + currentState.getCurrentPlayer().getColor();

                // Skip if we've already processed this state
                if (lastProcessedState.equals(currentStateString)) {
                    Thread.sleep(200);
                    continue;
                }

                // Check if it's our turn
                if (currentState.getCurrentPlayer().getColor() == aiColor) {
                    System.out.println("AI " + aiColor + "'s turn. Computing move...");
                    System.out.println("Current board state:\n" + currentState.toString());

                    // Get best move from minimax agent
                    Cell bestMove = agent.getBestMove(currentState);

                    if (bestMove != null) {
                        System.out.println("AI " + aiColor + " choosing move: (" +
                                bestMove.getCellRow() + ", " + bestMove.getCellColumn() + ")");

                        // Make the move
                        boolean success = currentState.takeAction(bestMove);

                        if (success) {
                            // Update our processed state
                            lastProcessedState = currentState.toString() + currentState.getCurrentPlayer().getColor();

                            // Write updated state back to file
                            FileProtocol.writeGameState(currentState);
                            System.out.println("Move completed and written to file");
                            System.out.println("New board state:\n" + currentState.toString());

                            // Reset error counter on successful move
                            consecutiveErrors = 0;
                        } else {
                            System.err.println("Failed to make move - move was invalid");
                            consecutiveErrors++;
                        }
                    } else {
                        System.err.println("No valid move found!");
                        // This might happen in rare cases, just continue
                        consecutiveErrors++;
                    }
                } else {
                    // Not our turn, update last processed state to avoid reprocessing
                    lastProcessedState = currentStateString;
                }

                // Prevent excessive CPU usage
                Thread.sleep(500);

            } catch (InterruptedException e) {
                System.out.println("AI Engine " + aiColor + " interrupted. Exiting...");
                break;
            } catch (Exception e) {
                System.err.println("Error in AI Engine " + aiColor + ": " + e.getMessage());
                e.printStackTrace();
                consecutiveErrors++;

                if (consecutiveErrors >= MAX_CONSECUTIVE_ERRORS) {
                    System.err.println("Too many consecutive errors. AI " + aiColor + " exiting...");
                    break;
                }

                // Wait a bit longer after errors
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException ie) {
                    break;
                }
            }
        }

        System.out.println("AI Engine " + aiColor + " shutting down.");
    }
}