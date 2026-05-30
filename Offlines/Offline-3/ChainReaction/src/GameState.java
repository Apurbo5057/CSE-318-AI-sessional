// This class is the representation of the game state at any given time .
// It contains parameters like :
// 1. The current player
// 2. Current Board Configuration
// 3. Board Number of Rows and Columns
// It contains Function like :
// 1. isGameOver()
// 2. IncreaseOrbCount(int column, int row)
// 3. ExplodeOrbs(int column, int row)
// 4. List of legal moves for this player at this state

import java.util.*;


public class GameState {



    public int NumColumn;
    public int NumRow;
    Player currentPlayer;
    List<Player> players;
    Cell[][] board;
    COLOR winner;
    boolean isGameOver = false;
    private int totalMoves = 0;
    final int directions[][] = {
            {-1, 0}, // Up
            {1, 0},  // Down
            {0, -1}, // Left
            {0, 1},  // Right
    };

    public GameState() {
        this.NumRow = 9;
        this.NumColumn = 6;
        this.currentPlayer = null;
        this.players = new ArrayList<>();
        this.board = new Cell[NumRow][NumColumn];
        for (int i = 0; i < NumRow; i++) {
            for (int j = 0; j < NumColumn; j++) {
                board[i][j] = new Cell(0, COLOR.WHITE, i, j, NumRow, NumColumn);
            }
        }
    }

    public GameState(GameState other) {
        this.NumColumn = other.NumColumn;
        this.NumRow = other.NumRow;
        this.currentPlayer = new Player(other.currentPlayer.getName(), other.currentPlayer.getColor() , other.currentPlayer.getType());
        this.board = new Cell[NumRow][NumColumn];
        this.players = other.players.stream()
                .map(player -> new Player(player.getName(), player.getColor(), player.getType()))
                .toList(); // Create a new list of players with copied data
        this.winner = other.winner;
        this.isGameOver = other.isGameOver;
        this.totalMoves = other.totalMoves;

        for (int i = 0; i < NumRow; i++) {
            for (int j = 0; j < NumColumn; j++) {
                Cell otherCell = other.board[i][j];
                this.board[i][j] = new Cell(otherCell.getOrbCount(), otherCell.getOrbColor(), i, j, NumRow, NumColumn);
            }
        }
    }

    // For the minimax agent
    public List<Cell> LegalActions() {
        List<Cell> legalMoves = new java.util.ArrayList<>();
        for (int i = 0; i < NumRow; i++) {
            for (int j = 0; j < NumColumn; j++) {
                if (board[i][j].canTakeAction(currentPlayer)) {
                    legalMoves.add(board[i][j]);
                }
            }
        }
        return legalMoves;
    }

    // Switch the current player to the next player
    public void switchPlayer() {
        if (currentPlayer.getColor() == COLOR.RED) {
            currentPlayer = players.get(1); // Switch to Player 2
        } else {
            currentPlayer = players.get(0); // Switch back to Player 1
        }
    }

    public void checkGameOver() {
        // Don't check for game over until both players have made at least one move
        if (totalMoves < 2) {
            return;
        }

        Set<COLOR> activeColors = new java.util.HashSet<>();
        for (int i = 0; i < NumRow; i++) {
            for (int j = 0; j < NumColumn; j++) {
                Cell cell = board[i][j];
                if (!cell.isEmpty()) {
                    activeColors.add(cell.getOrbColor());
                }
            }
        }

        // Game is over if only one color remains (and it's not WHITE)
        if (activeColors.size() == 1 && !activeColors.contains(COLOR.WHITE)) {
            winner = activeColors.iterator().next();
            isGameOver = true;
        }
    }

    public COLOR getWinner() {
        return winner;
    }

    // For player to take action on this cell
    public boolean takeAction(Cell cell) {
        if (cell.canTakeAction(currentPlayer)) {
            // Set the color to current player's color
            cell.setOrbColor(currentPlayer.getColor());
            cell.setOrbCount(cell.getOrbCount() + 1);

            // Increment move counter
            totalMoves++;

            // Handle all explosions that might occur BEFORE switching players
            handleAllExplosions();

            // Switch to the next player AFTER explosions are handled
            switchPlayer();

            // Check if the game is over
            checkGameOver();

            return true;
        }
        return false;
    }


    public boolean isValidCell(int row, int column) {
        return row >= 0 && row < NumRow && column >= 0 && column < NumColumn;
    }

    // Handle all explosions
    // loop until no more explosions found
    // For each explosion, update its neighbors orbcount and color and its own orb count
    // Handle all explosions

    private void handleAllExplosions() {
        while(true){
            boolean foundExplosion = false;
            for(int i=0;i<NumRow ;i++){
                for(int j=0; j<NumColumn ;j++){
                    Cell cell = board[i][j];
                    if(cell.willExplode()){
                        foundExplosion = true;
                        System.out.println("Explosion at cell (" + i + ", " + j + ") with color " + cell.getOrbColor());
                        //print orbcount and criticalmass
                        System.out.println( "Orb Count: " + cell.getOrbCount() + ", Critical Mass: " + cell.getCriticalMass());

                        // Store the exploding cell's color BEFORE clearing it
                        COLOR explodingColor = cell.getOrbColor();


                        // update neighbors orb count and color
                        for (int[] direction : directions){
                            int newRow = i + direction[0];
                            int newColumn = j + direction[1];
                            if (isValidCell(newRow, newColumn)) {
                                Cell neighbor = board[newRow][newColumn];
                                // Set neighbor's color to the EXPLODING cell's color (not current player's color)
                                neighbor.setOrbColor(explodingColor);
                                // Increment the neighbor's orb count
                                neighbor.setOrbCount(neighbor.getOrbCount() + 1);
                                // decrement own cell orb count
                                if( cell.getOrbCount() > 0) cell.setOrbCount(cell.getOrbCount() - 1);
                            }
                        }
                    }
                }
            }
            // If no explosion found, break the loop
            if (!foundExplosion) {
                break;
            }
        }
    }
    // Getters and setters
    public int getNumColumn() {
        return NumColumn;
    }

    public void setNumColumn(int numColumn) {
        NumColumn = numColumn;
    }

    public int getNumRow() {
        return NumRow;
    }

    public void setNumRow(int numRow) {
        NumRow = numRow;
    }

    public Player getCurrentPlayer() {
        return currentPlayer;
    }

    public void setCurrentPlayer(Player currentPlayer) {
        this.currentPlayer = currentPlayer;
    }

    public List<Player> getPlayers() {
        return players;
    }

    public void setPlayers(List<Player> players) {
        this.players = players;
    }

    public Cell[][] getBoard() {
        return board;
    }

    public void setBoard(Cell[][] board) {
        this.board = board;
    }

    public boolean isGameOver() {
        return isGameOver;
    }

    public void setGameOver(boolean gameOver) {
        isGameOver = gameOver;
    }


    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < NumRow; i++) {
            for (int j = 0; j < NumColumn; j++) {
                Cell cell = board[i][j];
                if (cell.isEmpty()) {
                    sb.append("0");
                } else {
                    sb.append(cell.getOrbCount());
                    sb.append(cell.getOrbColor() == COLOR.RED ? "R" : "B");
                }
                if (j < NumColumn - 1) {
                    sb.append(" ");
                }
            }
            if (i < NumRow - 1) {
                sb.append("\n");
            }
        }
        return sb.toString();
    }
}