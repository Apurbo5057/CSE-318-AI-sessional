import java.util.List;

public class MiniMaxAgent {
    private Heuristic heuristic;
    private int maxDepth;
    private COLOR aiColor;

    public MiniMaxAgent(COLOR aiColor, int maxDepth) {
        this.aiColor = aiColor;
        this.maxDepth = maxDepth;
        this.heuristic = new Heuristic();
    }

    public static class MoveResult {
        public Cell bestMove;
        public double value;

        public MoveResult(Cell move, double value) {
            this.bestMove = move;
            this.value = value;
        }
    }

    public MoveResult minimax(GameState state, int depth) {
        return minimaxWithAlphaBeta(state, depth, Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY, true);
    }

    private MoveResult minimaxWithAlphaBeta(GameState state, int depth, double alpha, double beta, boolean isMaximizing) {
        // Terminal conditions
        if (depth == 0 || state.isGameOver()) {
            double eval = heuristic.evaluate(state, aiColor);
            return new MoveResult(null, eval);
        }

        List<Cell> legalMoves = state.LegalActions();
        if (legalMoves.isEmpty()) {
            double eval = heuristic.evaluate(state, aiColor);
            return new MoveResult(null, eval);
        }

        Cell bestMove = legalMoves.get(0);

        if (isMaximizing) {
            double maxEval = Double.NEGATIVE_INFINITY;

            for (Cell move : legalMoves) {
                // Create a copy of the state
                GameState newState = new GameState(state);

                // Make the move
                Cell newStateCell = newState.getBoard()[move.getCellRow()][move.getCellColumn()];
                newState.takeAction(newStateCell);

                // Recursively evaluate
                MoveResult result = minimaxWithAlphaBeta(newState, depth - 1, alpha, beta, false);

                if (result.value > maxEval) {
                    maxEval = result.value;
                    bestMove = move;
                }

                alpha = Math.max(alpha, result.value);
                if (beta <= alpha) {
                    break; // Alpha-beta pruning
                }
            }

            return new MoveResult(bestMove, maxEval);
        } else {
            double minEval = Double.POSITIVE_INFINITY;

            for (Cell move : legalMoves) {
                // Create a copy of the state
                GameState newState = new GameState(state);

                // Make the move
                Cell newStateCell = newState.getBoard()[move.getCellRow()][move.getCellColumn()];
                newState.takeAction(newStateCell);

                // Recursively evaluate
                MoveResult result = minimaxWithAlphaBeta(newState, depth - 1, alpha, beta, true);

                if (result.value < minEval) {
                    minEval = result.value;
                    bestMove = move;
                }

                beta = Math.min(beta, result.value);
                if (beta <= alpha) {
                    break; // Alpha-beta pruning
                }
            }

            return new MoveResult(bestMove, minEval);
        }
    }

    public Cell getBestMove(GameState state) {
        MoveResult result = minimax(state, maxDepth);
        return result.bestMove;
    }
    public void setHeuristic(Heuristic heuristic) {
        this.heuristic = heuristic;
    }
}