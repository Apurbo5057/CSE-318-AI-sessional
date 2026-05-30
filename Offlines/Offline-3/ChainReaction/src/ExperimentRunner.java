import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class ExperimentRunner {

    public static class GameResult {
        public COLOR winner;
        public int totalMoves;
        public long gameTimeMs;
        public Heuristic.HeuristicType heuristic1;
        public Heuristic.HeuristicType heuristic2;

        public GameResult(COLOR winner, int moves, long time,
                          Heuristic.HeuristicType h1, Heuristic.HeuristicType h2) {
            this.winner = winner;
            this.totalMoves = moves;
            this.gameTimeMs = time;
            this.heuristic1 = h1;
            this.heuristic2 = h2;
        }
    }

    public static void runExperiments() {
        System.out.println("=== Chain Reaction Heuristic Analysis ===\n");

        // Test each heuristic against random
        Heuristic.HeuristicType[] heuristics = {
                Heuristic.HeuristicType.ORB_COUNT_DIFFERENCE,
                Heuristic.HeuristicType.WEIGHTED_POSITION,
                Heuristic.HeuristicType.CRITICAL_MASS_THREAT,
                Heuristic.HeuristicType.CHAIN_POTENTIAL,
                Heuristic.HeuristicType.COMBINED
        };

        // Test different depths
        int[] depths = {2, 3, 4, 5};

        for (Heuristic.HeuristicType heuristic : heuristics) {
            System.out.println("Testing: " + heuristic);

            for (int depth : depths) {
                List<GameResult> results = new ArrayList<>();
                int wins = 0;
                long totalTime = 0;
                int totalMoves = 0;

                // Run 10 games for each configuration
                for (int game = 0; game < 10; game++) {
                    GameResult result = playGame(heuristic, depth, true);
                    results.add(result);

                    if (result.winner == COLOR.RED) wins++; // Assuming RED is our AI
                    totalTime += result.gameTimeMs;
                    totalMoves += result.totalMoves;
                }

                // Print results
                System.out.printf("Depth %d: Win Rate: %.1f%%, Avg Time: %.2fs, Avg Moves: %.1f\n",
                        depth, (wins/10.0)*100, totalTime/10000.0, totalMoves/10.0);
            }
            System.out.println();
        }

        // Heuristic vs Heuristic matches
        System.out.println("=== Heuristic Head-to-Head Results ===");
        for (int i = 0; i < heuristics.length; i++) {
            for (int j = i+1; j < heuristics.length; j++) {
                testHeuristicVsHeuristic(heuristics[i], heuristics[j]);
            }
        }
    }

    private static GameResult playGame(Heuristic.HeuristicType aiHeuristic, int depth, boolean vsRandom) {
        long startTime = System.currentTimeMillis();
        GameState state = new GameState();
        MiniMaxAgent aiAgent = new MiniMaxAgent(COLOR.RED, depth);
        aiAgent.setHeuristic(new Heuristic(aiHeuristic));
        RandomAgent randomAgent = new RandomAgent();

        int moves = 0;

        while (!state.isGameOver() && moves < 200) { // Max 200 moves to prevent infinite games
            if (state.getCurrentPlayer().getColor() == COLOR.RED) {
                // AI move
                Cell move = aiAgent.getBestMove(state);
                if (move != null) {
                    state.takeAction(move);
                }
            } else {
                // Random or second AI move
                if (vsRandom) {
                    Cell move = randomAgent.getRandomMove(state);
                    if (move != null) {
                        state.takeAction(move);
                    }
                }
            }
            moves++;
        }

        long endTime = System.currentTimeMillis();
        return new GameResult(state.getWinner(), moves, endTime - startTime, aiHeuristic, null);
    }

    private static void testHeuristicVsHeuristic(Heuristic.HeuristicType h1, Heuristic.HeuristicType h2) {
        int h1Wins = 0;
        int totalGames = 20;

        for (int game = 0; game < totalGames; game++) {
            GameState state = new GameState();
            MiniMaxAgent agent1 = new MiniMaxAgent(COLOR.RED, 4);
            MiniMaxAgent agent2 = new MiniMaxAgent(COLOR.BLUE, 4);
            agent1.setHeuristic(new Heuristic(h1));
            agent2.setHeuristic(new Heuristic(h2));

            int moves = 0;
            while (!state.isGameOver() && moves < 200) {
                if (state.getCurrentPlayer().getColor() == COLOR.RED) {
                    Cell move = agent1.getBestMove(state);
                    if (move != null) state.takeAction(move);
                } else {
                    Cell move = agent2.getBestMove(state);
                    if (move != null) state.takeAction(move);
                }
                moves++;
            }

            if (state.getWinner() == COLOR.RED) h1Wins++;
        }

        System.out.printf("%s vs %s: %.1f%% win rate\n",
                h1, h2, (h1Wins/(double)totalGames)*100);
    }
}

// You'll also need a RandomAgent class
class RandomAgent {
    private Random random = new Random();

    public Cell getRandomMove(GameState state) {
        List<Cell> legalMoves = state.LegalActions();
        if (legalMoves.isEmpty()) return null;
        return legalMoves.get(random.nextInt(legalMoves.size()));
    }
}