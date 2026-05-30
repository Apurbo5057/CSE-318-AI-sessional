// GameHeuristics.java
public class GameHeuristics {

    public static double evaluateState(GameState state, char aiPlayer) {
        if (state.isGameOver()) {
            if (state.getWinner() == aiPlayer) return 10000;
            else if (state.getWinner() != GameState.EMPTY) return -10000;
            else return 0;
        }

        // Combine multiple heuristics
        double score = 0;
        score += 2.0 * orbCountHeuristic(state, aiPlayer);
        score += 1.5 * territoryControlHeuristic(state, aiPlayer);
        score += 1.0 * stabilityHeuristic(state, aiPlayer);
        score += 0.8 * positionValueHeuristic(state, aiPlayer);
        score += 1.2 * threatAssessmentHeuristic(state, aiPlayer);

        return score;
    }

    // Heuristic 1: Simple orb count difference
    public static double orbCountHeuristic(GameState state, char aiPlayer) {
        int aiOrbs = 0, opponentOrbs = 0;
        char opponent = (aiPlayer == GameState.RED) ? GameState.BLUE : GameState.RED;

        for (int i = 0; i < GameState.ROWS; i++) {
            for (int j = 0; j < GameState.COLS; j++) {
                GameState.Cell cell = state.getCell(i, j);
                if (cell.color == aiPlayer) {
                    aiOrbs += cell.count;
                } else if (cell.color == opponent) {
                    opponentOrbs += cell.count;
                }
            }
        }

        return aiOrbs - opponentOrbs;
    }

    // Heuristic 2: Territory control (number of cells controlled)
    public static double territoryControlHeuristic(GameState state, char aiPlayer) {
        int aiCells = 0, opponentCells = 0;
        char opponent = (aiPlayer == GameState.RED) ? GameState.BLUE : GameState.RED;

        for (int i = 0; i < GameState.ROWS; i++) {
            for (int j = 0; j < GameState.COLS; j++) {
                GameState.Cell cell = state.getCell(i, j);
                if (cell.color == aiPlayer) {
                    aiCells++;
                } else if (cell.color == opponent) {
                    opponentCells++;
                }
            }
        }

        return aiCells - opponentCells;
    }

    // Heuristic 3: Stability (cells close to critical mass are unstable)
    public static double stabilityHeuristic(GameState state, char aiPlayer) {
        double aiStability = 0, opponentStability = 0;
        char opponent = (aiPlayer == GameState.RED) ? GameState.BLUE : GameState.RED;

        for (int i = 0; i < GameState.ROWS; i++) {
            for (int j = 0; j < GameState.COLS; j++) {
                GameState.Cell cell = state.getCell(i, j);
                if (!cell.isEmpty()) {
                    int criticalMass = state.getCriticalMass(i, j);
                    double stability = (double)(criticalMass - cell.count) / criticalMass;

                    if (cell.color == aiPlayer) {
                        aiStability += stability;
                    } else if (cell.color == opponent) {
                        opponentStability += stability;
                    }
                }
            }
        }

        return aiStability - opponentStability;
    }

    // Heuristic 4: Position value (corners and edges are more valuable)
    public static double positionValueHeuristic(GameState state, char aiPlayer) {
        double aiValue = 0, opponentValue = 0;
        char opponent = (aiPlayer == GameState.RED) ? GameState.BLUE : GameState.RED;

        for (int i = 0; i < GameState.ROWS; i++) {
            for (int j = 0; j < GameState.COLS; j++) {
                GameState.Cell cell = state.getCell(i, j);
                if (!cell.isEmpty()) {
                    double positionValue = getPositionValue(i, j);

                    if (cell.color == aiPlayer) {
                        aiValue += positionValue * cell.count;
                    } else if (cell.color == opponent) {
                        opponentValue += positionValue * cell.count;
                    }
                }
            }
        }

        return aiValue - opponentValue;
    }

    // Heuristic 5: Threat assessment (potential chain reactions)
    public static double threatAssessmentHeuristic(GameState state, char aiPlayer) {
        double aiThreats = 0, opponentThreats = 0;
        char opponent = (aiPlayer == GameState.RED) ? GameState.BLUE : GameState.RED;

        for (int i = 0; i < GameState.ROWS; i++) {
            for (int j = 0; j < GameState.COLS; j++) {
                GameState.Cell cell = state.getCell(i, j);
                if (!cell.isEmpty()) {
                    int criticalMass = state.getCriticalMass(i, j);
                    if (cell.count == criticalMass - 1) {
                        // This cell is one orb away from exploding
                        double threatValue = countAdjacentOpponents(state, i, j, cell.color);

                        if (cell.color == aiPlayer) {
                            aiThreats += threatValue;
                        } else if (cell.color == opponent) {
                            opponentThreats += threatValue;
                        }
                    }
                }
            }
        }

        return aiThreats - opponentThreats;
    }

    private static double getPositionValue(int row, int col) {
        // Corner cells are most valuable (critical mass = 2)
        if ((row == 0 || row == GameState.ROWS - 1) &&
                (col == 0 || col == GameState.COLS - 1)) {
            return 3.0;
        }
        // Edge cells (critical mass = 3)
        else if (row == 0 || row == GameState.ROWS - 1 ||
                col == 0 || col == GameState.COLS - 1) {
            return 2.0;
        }
        // Center cells (critical mass = 4)
        else {
            return 1.0;
        }
    }

    private static int countAdjacentOpponents(GameState state, int row, int col, char color) {
        char opponent = (color == GameState.RED) ? GameState.BLUE : GameState.RED;
        int count = 0;

        int[][] directions = {{-1, 0}, {1, 0}, {0, -1}, {0, 1}};
        for (int[] dir : directions) {
            int newRow = row + dir[0];
            int newCol = col + dir[1];
            if (newRow >= 0 && newRow < GameState.ROWS &&
                    newCol >= 0 && newCol < GameState.COLS) {
                GameState.Cell cell = state.getCell(newRow, newCol);
                if (cell.color == opponent) {
                    count += cell.count;
                }
            }
        }

        return count;
    }
}

