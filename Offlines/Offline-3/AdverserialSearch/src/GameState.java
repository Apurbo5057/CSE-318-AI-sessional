// GameState.java
import java.io.*;
import java.util.*;

public class GameState {
    public static final int ROWS = 9;
    public static final int COLS = 6;
    public static final char RED = 'R';
    public static final char BLUE = 'B';
    public static final char EMPTY = '0';

    private Cell[][] board;
    private char currentPlayer;
    private boolean gameOver;
    private char winner;

    public static class Cell {
        public int count;
        public char color;

        public Cell() {
            this.count = 0;
            this.color = EMPTY;
        }

        public Cell(int count, char color) {
            this.count = count;
            this.color = color;
        }

        public Cell copy() {
            return new Cell(count, color);
        }

        public boolean isEmpty() {
            return count == 0;
        }

        @Override
        public String toString() {
            return isEmpty() ? "0" : count + "" + color;
        }
    }

    public GameState() {
        board = new Cell[ROWS][COLS];
        for (int i = 0; i < ROWS; i++) {
            for (int j = 0; j < COLS; j++) {
                board[i][j] = new Cell();
            }
        }
        currentPlayer = RED;
        gameOver = false;
        winner = EMPTY;
    }

    public GameState(GameState other) {
        board = new Cell[ROWS][COLS];
        for (int i = 0; i < ROWS; i++) {
            for (int j = 0; j < COLS; j++) {
                board[i][j] = other.board[i][j].copy();
            }
        }
        currentPlayer = other.currentPlayer;
        gameOver = other.gameOver;
        winner = other.winner;
    }

    public Cell getCell(int row, int col) {
        return board[row][col];
    }

    public char getCurrentPlayer() {
        return currentPlayer;
    }

    public boolean isGameOver() {
        return gameOver;
    }

    public char getWinner() {
        return winner;
    }

    public int getCriticalMass(int row, int col) {
        int neighbors = 0;
        if (row > 0) neighbors++;
        if (row < ROWS - 1) neighbors++;
        if (col > 0) neighbors++;
        if (col < COLS - 1) neighbors++;
        return neighbors;
    }

    public boolean isValidMove(int row, int col) {
        if (row < 0 || row >= ROWS || col < 0 || col >= COLS) return false;
        Cell cell = board[row][col];
        return cell.isEmpty() || cell.color == currentPlayer;
    }

    public List<int[]> getLegalMoves() {
        List<int[]> moves = new ArrayList<>();
        for (int i = 0; i < ROWS; i++) {
            for (int j = 0; j < COLS; j++) {
                if (isValidMove(i, j)) {
                    moves.add(new int[]{i, j});
                }
            }
        }
        return moves;
    }

    public void makeMove(int row, int col) {
        if (!isValidMove(row, col)) return;

        board[row][col].count++;
        board[row][col].color = currentPlayer;

        explode();
        checkGameOver();
        switchPlayer();
    }

    private void explode() {
        boolean hasExplosion = true;
        while (hasExplosion) {
            hasExplosion = false;
            for (int i = 0; i < ROWS; i++) {
                for (int j = 0; j < COLS; j++) {
                    if (board[i][j].count >= getCriticalMass(i, j) && board[i][j].count > 0) {
                        hasExplosion = true;
                        explodeCell(i, j);
                    }
                }
            }
        }
    }

    private void explodeCell(int row, int col) {
        Cell cell = board[row][col];
        char color = cell.color;
        int orbsToDistribute = cell.count;

        cell.count = 0;
        cell.color = EMPTY;

        int[][] directions = {{-1, 0}, {1, 0}, {0, -1}, {0, 1}};
        for (int[] dir : directions) {
            int newRow = row + dir[0];
            int newCol = col + dir[1];
            if (newRow >= 0 && newRow < ROWS && newCol >= 0 && newCol < COLS) {
                board[newRow][newCol].count++;
                board[newRow][newCol].color = color;
            }
        }
    }

    private void checkGameOver() {
        Set<Character> playersOnBoard = new HashSet<>();
        for (int i = 0; i < ROWS; i++) {
            for (int j = 0; j < COLS; j++) {
                if (!board[i][j].isEmpty()) {
                    playersOnBoard.add(board[i][j].color);
                }
            }
        }

        if (playersOnBoard.size() <= 1) {
            gameOver = true;
            winner = playersOnBoard.isEmpty() ? EMPTY : playersOnBoard.iterator().next();
        }
    }

    private void switchPlayer() {
        currentPlayer = (currentPlayer == RED) ? BLUE : RED;
    }

    public void saveToFile(String filename, boolean isHumanMove) throws IOException {
        try (PrintWriter writer = new PrintWriter(new FileWriter(filename))) {
            writer.println(isHumanMove ? "Human Move:" : "AI Move:");
            for (int i = 0; i < ROWS; i++) {
                for (int j = 0; j < COLS; j++) {
                    writer.print(board[i][j].toString());
                    if (j < COLS - 1) writer.print(" ");
                }
                writer.println();
            }
        }
    }

    public static GameState loadFromFile(String filename) throws IOException {
        GameState state = new GameState();
        try (BufferedReader reader = new BufferedReader(new FileReader(filename))) {
            String header = reader.readLine();
            if (header.equals("Human Move:")) {
                state.currentPlayer = BLUE; // AI's turn
            } else {
                state.currentPlayer = RED; // Human's turn
            }

            for (int i = 0; i < ROWS; i++) {
                String line = reader.readLine();
                String[] cells = line.split(" ");
                for (int j = 0; j < COLS; j++) {
                    String cellStr = cells[j];
                    if (cellStr.equals("0")) {
                        state.board[i][j] = new Cell();
                    } else {
                        int count = Integer.parseInt(cellStr.substring(0, cellStr.length() - 1));
                        char color = cellStr.charAt(cellStr.length() - 1);
                        state.board[i][j] = new Cell(count, color);
                    }
                }
            }
        }
        state.checkGameOver();
        return state;
    }
}

