public class Heuristic {

    public enum HeuristicType {
        ORB_COUNT_DIFFERENCE,
        WEIGHTED_POSITION,
        CRITICAL_MASS_THREAT,
        CHAIN_POTENTIAL,
        COMBINED
    }

    private HeuristicType currentHeuristic = HeuristicType.COMBINED;

    public Heuristic() {}

    public Heuristic(HeuristicType type) {
        this.currentHeuristic = type;
    }

    public double evaluate(GameState state, COLOR aiColor) {
        // Check for terminal states first
        if (state.isGameOver()) {
            if (state.getWinner() == aiColor) {
                return 10000; // AI wins
            } else {
                return -10000; // AI loses
            }
        }

        switch (currentHeuristic) {
            case ORB_COUNT_DIFFERENCE:
                return orbCountDifference(state, aiColor);
            case WEIGHTED_POSITION:
                return weightedPositionEvaluation(state, aiColor);
            case CRITICAL_MASS_THREAT:
                return criticalMassThreatEvaluation(state, aiColor);
            case CHAIN_POTENTIAL:
                return chainPotentialEvaluation(state, aiColor);
            case COMBINED:
                return combinedEvaluation(state, aiColor);
            default:
                return orbCountDifference(state, aiColor);
        }
    }

    /**
     * Heuristic 1: Simple orb count difference
     * Returns: AI_orbs - Opponent_orbs
     */
    private double orbCountDifference(GameState state, COLOR aiColor) {
        int aiOrbs = 0;
        int opponentOrbs = 0;
        COLOR opponentColor = (aiColor == COLOR.RED) ? COLOR.BLUE : COLOR.RED;

        Cell[][] board = state.getBoard();
        for (int i = 0; i < state.NumRow; i++) {
            for (int j = 0; j < state.NumColumn; j++) {
                Cell cell = board[i][j];
                if (!cell.isEmpty()) {
                    if (cell.getOrbColor() == aiColor) {
                        aiOrbs += cell.getOrbCount();
                    } else if (cell.getOrbColor() == opponentColor) {
                        opponentOrbs += cell.getOrbCount();
                    }
                }
            }
        }

        return aiOrbs - opponentOrbs;
    }

    /**
     * Heuristic 2: Weighted position evaluation
     * Corner cells are more valuable, edge cells medium, center cells less valuable
     */
    private double weightedPositionEvaluation(GameState state, COLOR aiColor) {
        double aiScore = 0;
        double opponentScore = 0;
        COLOR opponentColor = (aiColor == COLOR.RED) ? COLOR.BLUE : COLOR.RED;

        Cell[][] board = state.getBoard();
        for (int i = 0; i < state.NumRow; i++) {
            for (int j = 0; j < state.NumColumn; j++) {
                Cell cell = board[i][j];
                if (!cell.isEmpty()) {
                    double weight = getPositionWeight(i, j, state.NumRow, state.NumColumn);
                    double cellValue = cell.getOrbCount() * weight;

                    if (cell.getOrbColor() == aiColor) {
                        aiScore += cellValue;
                    } else if (cell.getOrbColor() == opponentColor) {
                        opponentScore += cellValue;
                    }
                }
            }
        }

        return aiScore - opponentScore;
    }

    private double getPositionWeight(int row, int col, int numRows, int numCols) {
        // Corner cells (critical mass = 2) are most valuable
        if ((row == 0 || row == numRows - 1) && (col == 0 || col == numCols - 1)) {
            return 3.0;
        }
        // Edge cells (critical mass = 3) are medium value
        else if (row == 0 || row == numRows - 1 || col == 0 || col == numCols - 1) {
            return 2.0;
        }
        // Center cells (critical mass = 4) are least valuable
        else {
            return 1.0;
        }
    }

    /**
     * Heuristic 3: Critical mass threat evaluation
     * Considers cells that are close to exploding
     */
    private double criticalMassThreatEvaluation(GameState state, COLOR aiColor) {
        double aiScore = 0;
        double opponentScore = 0;
        COLOR opponentColor = (aiColor == COLOR.RED) ? COLOR.BLUE : COLOR.RED;

        Cell[][] board = state.getBoard();
        for (int i = 0; i < state.NumRow; i++) {
            for (int j = 0; j < state.NumColumn; j++) {
                Cell cell = board[i][j];
                if (!cell.isEmpty()) {
                    int orbCount = cell.getOrbCount();
                    int criticalMass = cell.getCriticalMass();

                    // Higher score for cells closer to critical mass
                    double threatLevel = (double) orbCount / criticalMass;
                    double cellScore = orbCount * (1 + threatLevel);

                    if (cell.getOrbColor() == aiColor) {
                        aiScore += cellScore;
                    } else if (cell.getOrbColor() == opponentColor) {
                        opponentScore += cellScore;
                    }
                }
            }
        }

        return aiScore - opponentScore;
    }

    /**
     * Heuristic 4: Chain potential evaluation
     * Considers the potential for chain reactions
     */
    private double chainPotentialEvaluation(GameState state, COLOR aiColor) {
        double aiScore = 0;
        double opponentScore = 0;
        COLOR opponentColor = (aiColor == COLOR.RED) ? COLOR.BLUE : COLOR.RED;

        Cell[][] board = state.getBoard();
        final int[][] directions = {{-1, 0}, {1, 0}, {0, -1}, {0, 1}};

        for (int i = 0; i < state.NumRow; i++) {
            for (int j = 0; j < state.NumColumn; j++) {
                Cell cell = board[i][j];
                if (!cell.isEmpty()) {
                    double chainPotential = calculateChainPotential(board, i, j, directions, state);

                    if (cell.getOrbColor() == aiColor) {
                        aiScore += cell.getOrbCount() + chainPotential;
                    } else if (cell.getOrbColor() == opponentColor) {
                        opponentScore += cell.getOrbCount() + chainPotential;
                    }
                }
            }
        }

        return aiScore - opponentScore;
    }

    private double calculateChainPotential(Cell[][] board, int row, int col, int[][] directions, GameState state) {
        double potential = 0;
        Cell cell = board[row][col];

        // If cell is close to critical mass, check neighbors
        if (cell.getOrbCount() >= cell.getCriticalMass() - 1) {
            for (int[] dir : directions) {
                int newRow = row + dir[0];
                int newCol = col + dir[1];

                if (newRow >= 0 && newRow < state.NumRow && newCol >= 0 && newCol < state.NumColumn) {
                    Cell neighbor = board[newRow][newCol];
                    // If neighbor is also close to critical mass, increase potential
                    if (!neighbor.isEmpty() && neighbor.getOrbCount() >= neighbor.getCriticalMass() - 2) {
                        potential += 0.5;
                    }
                }
            }
        }

        return potential;
    }

    /**
     * Heuristic 5: Combined evaluation using weighted sum of all heuristics
     */
    private double combinedEvaluation(GameState state, COLOR aiColor) {
        double orbDiff = orbCountDifference(state, aiColor);
        double weighted = weightedPositionEvaluation(state, aiColor);
        double threat = criticalMassThreatEvaluation(state, aiColor);
        double chain = chainPotentialEvaluation(state, aiColor);

        // Weighted combination
        return 0.3 * orbDiff + 0.2 * weighted + 0.3 * threat + 0.2 * chain;
    }

    public void setHeuristicType(HeuristicType type) {
        this.currentHeuristic = type;
    }

    public HeuristicType getCurrentHeuristic() {
        return currentHeuristic;
    }
}