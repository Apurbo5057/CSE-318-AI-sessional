import java.util.List;

// MinimaxAgent.java
public class MinimaxAgent {
    private char aiPlayer;
    private int maxDepth;

    public MinimaxAgent(char aiPlayer, int maxDepth) {
        this.aiPlayer = aiPlayer;
        this.maxDepth = maxDepth;
    }

    public int[] findBestMove(GameState state) {
        MinimaxResult result = minimax(state, 0, Double.NEGATIVE_INFINITY,
                Double.POSITIVE_INFINITY, true);
        return result.move;
    }

    private MinimaxResult minimax(GameState state, int depth, double alpha,
                                  double beta, boolean maximizingPlayer) {
        if (depth >= maxDepth || state.isGameOver()) {
            double value = GameHeuristics.evaluateState(state, aiPlayer);
            return new MinimaxResult(value, null);
        }

        List<int[]> legalMoves = state.getLegalMoves();
        if (legalMoves.isEmpty()) {
            double value = GameHeuristics.evaluateState(state, aiPlayer);
            return new MinimaxResult(value, null);
        }

        int[] bestMove = legalMoves.get(0);

        if (maximizingPlayer) {
            double maxEval = Double.NEGATIVE_INFINITY;

            for (int[] move : legalMoves) {
                GameState newState = new GameState(state);
                newState.makeMove(move[0], move[1]);

                MinimaxResult result = minimax(newState, depth + 1, alpha, beta, false);

                if (result.value > maxEval) {
                    maxEval = result.value;
                    bestMove = move;
                }

                alpha = Math.max(alpha, result.value);
                if (beta <= alpha) {
                    break; // Alpha-beta pruning
                }
            }

            return new MinimaxResult(maxEval, bestMove);
        } else {
            double minEval = Double.POSITIVE_INFINITY;

            for (int[] move : legalMoves) {
                GameState newState = new GameState(state);
                newState.makeMove(move[0], move[1]);

                MinimaxResult result = minimax(newState, depth + 1, alpha, beta, true);

                if (result.value < minEval) {
                    minEval = result.value;
                    bestMove = move;
                }

                beta = Math.min(beta, result.value);
                if (beta <= alpha) {
                    break; // Alpha-beta pruning
                }
            }

            return new MinimaxResult(minEval, bestMove);
        }
    }

    private static class MinimaxResult {
        double value;
        int[] move;

        MinimaxResult(double value, int[] move) {
            this.value = value;
            this.move = move;
        }
    }
}
