import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import javafx.concurrent.Task;

import java.io.IOException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class MainWithFileProtocol extends Application {

    // Board dimensions (9 rows x 6 columns as per assignment)
    private static final int ROWS = 9;
    private static final int COLS = 6;
    private static final int CELL_SIZE = 80;

    private GameState gameState;
    private GridPane boardGrid;
    private Label currentPlayerLabel;
    private Label gameStatusLabel;
    private Button[][] cellButtons;
    private ScheduledExecutorService fileWatcher;
    private GameState.PlayerType player1Type;
    private GameState.PlayerType player2Type;
    private Process aiProcess1;
    private Process aiProcess2;

    // Add these fields to track file state
    private String lastFileContent = "";
    private boolean isProcessingFileUpdate = false;
    private long lastMoveTime = 0;

    @Override
    public void start(Stage primaryStage) {
        // Show player selection dialog first
        PlayerSelectionDialog.PlayerConfiguration config = PlayerSelectionDialog.showDialog(primaryStage);

        if (config.cancelled) {
            System.exit(0);
            return;
        }

        player1Type = config.player1Type;
        player2Type = config.player2Type;

        // Initialize game state
        gameState = new GameState();
        gameState.setPlayer1Type(player1Type);
        gameState.setPlayer2Type(player2Type);

        // Create main layout
        VBox mainLayout = new VBox(10);
        mainLayout.setAlignment(Pos.CENTER);
        mainLayout.setStyle("-fx-padding: 20; -fx-background-color: #2c3e50;");

        // Create header
        VBox header = createHeader();

        // Create game board
        boardGrid = createGameBoard();

        // Create control panel
        HBox controlPanel = createControlPanel();

        // Add all components to main layout
        mainLayout.getChildren().addAll(header, boardGrid, controlPanel);

        // Create scene and show stage
        Scene scene = new Scene(mainLayout, COLS * CELL_SIZE + 100, ROWS * CELL_SIZE + 200);
        primaryStage.setTitle("Chain Reaction Game - File Protocol");
        primaryStage.setScene(scene);
        primaryStage.setResizable(false);
        primaryStage.show();

        // Initialize the game
        initializeGame();

        // Set up shutdown hook
        primaryStage.setOnCloseRequest(e -> {
            cleanup();
            Platform.exit();
            System.exit(0);
        });
    }

    private void initializeGame() {
        // Clear any existing file
        try {
            java.nio.file.Files.deleteIfExists(java.nio.file.Paths.get("gamestate.txt"));
        } catch (Exception e) {
            // Ignore
        }

        // Write initial game state to file
        FileProtocol.writeGameState(gameState);
        lastFileContent = gameState.toString();
        lastMoveTime = System.currentTimeMillis();

        // Start AI processes if needed
        startAIProcesses();

        // Start file watcher only if there are AI players
        if (player1Type == GameState.PlayerType.AI || player2Type == GameState.PlayerType.AI) {
            startFileWatcher();
        }

        // Update display
        updateBoard();
        updateCurrentPlayerLabel();
    }

    private void startAIProcesses() {
        try {
            if (player1Type == GameState.PlayerType.AI) {
                System.out.println("Starting AI process for RED player...");
                ProcessBuilder pb1 = new ProcessBuilder("java", "-cp", ".", "GameEngine", "RED", "4");
                pb1.inheritIO(); // This will help debug AI process issues
                aiProcess1 = pb1.start();
                System.out.println("Started AI process for RED player");
            }

            if (player2Type == GameState.PlayerType.AI) {
                System.out.println("Starting AI process for BLUE player...");
                ProcessBuilder pb2 = new ProcessBuilder("java", "-cp", ".", "GameEngine", "BLUE", "4");
                pb2.inheritIO(); // This will help debug AI process issues
                aiProcess2 = pb2.start();
                System.out.println("Started AI process for BLUE player");
            }
        } catch (IOException e) {
            System.err.println("Failed to start AI processes: " + e.getMessage());
            e.printStackTrace();
            showAlert("Error", "Failed to start AI processes. Make sure GameEngine.class is compiled and in classpath.");
        }
    }

    private void startFileWatcher() {
        fileWatcher = Executors.newSingleThreadScheduledExecutor();
        fileWatcher.scheduleAtFixedRate(() -> {
            try {
                if (FileProtocol.fileExists() && !isProcessingFileUpdate) {
                    GameState newState = FileProtocol.readGameState();
                    if (newState != null) {
                        String newContent = newState.toString();

                        // Only update if content actually changed and enough time has passed
                        if (!lastFileContent.equals(newContent) &&
                                (System.currentTimeMillis() - lastMoveTime > 1000)) {

                            Platform.runLater(() -> {
                                if (!isProcessingFileUpdate) {
                                    isProcessingFileUpdate = true;
                                    System.out.println("File content changed, updating game state");
                                    gameState = newState;
                                    lastFileContent = newContent;
                                    lastMoveTime = System.currentTimeMillis();
                                    updateBoard();
                                    updateCurrentPlayerLabel();
                                    checkGameOver();
                                    isProcessingFileUpdate = false;
                                }
                            });
                        }
                    }
                }
            } catch (Exception e) {
                System.err.println("Error watching file: " + e.getMessage());
                e.printStackTrace();
            }
        }, 1000, 1000, TimeUnit.MILLISECONDS); // Increased interval to 1 second
    }

    private VBox createHeader() {
        VBox header = new VBox(10);
        header.setAlignment(Pos.CENTER);

        // Game title
        Text title = new Text("Chain Reaction");
        title.setFont(Font.font("Arial", FontWeight.BOLD, 24));
        title.setFill(Color.WHITE);

        // Player type info
        String p1TypeStr = player1Type == GameState.PlayerType.HUMAN ? "Human" : "AI";
        String p2TypeStr = player2Type == GameState.PlayerType.HUMAN ? "Human" : "AI";
        Text gameInfo = new Text("Red: " + p1TypeStr + " vs Blue: " + p2TypeStr);
        gameInfo.setFont(Font.font("Arial", FontWeight.NORMAL, 14));
        gameInfo.setFill(Color.LIGHTGRAY);

        // Current player label
        currentPlayerLabel = new Label();
        currentPlayerLabel.setFont(Font.font("Arial", FontWeight.BOLD, 16));
        updateCurrentPlayerLabel();

        // Game status label
        gameStatusLabel = new Label("Game in Progress");
        gameStatusLabel.setFont(Font.font("Arial", FontWeight.NORMAL, 14));
        gameStatusLabel.setTextFill(Color.LIGHTGRAY);

        header.getChildren().addAll(title, gameInfo, currentPlayerLabel, gameStatusLabel);
        return header;
    }

    private GridPane createGameBoard() {
        GridPane grid = new GridPane();
        grid.setAlignment(Pos.CENTER);
        grid.setHgap(2);
        grid.setVgap(2);
        grid.setStyle("-fx-background-color: #34495e; -fx-padding: 10;");

        cellButtons = new Button[ROWS][COLS];

        for (int row = 0; row < ROWS; row++) {
            for (int col = 0; col < COLS; col++) {
                Button cellButton = createCellButton(row, col);
                cellButtons[row][col] = cellButton;
                grid.add(cellButton, col, row);
            }
        }

        return grid;
    }

    private Button createCellButton(int row, int col) {
        Button button = new Button();
        button.setPrefSize(CELL_SIZE, CELL_SIZE);
        button.setStyle("-fx-background-color: #ecf0f1; -fx-border-color: #bdc3c7; -fx-border-width: 2;");

        // Add click handler
        button.setOnAction(e -> handleCellClick(row, col));

        return button;
    }

    private HBox createControlPanel() {
        HBox controlPanel = new HBox(20);
        controlPanel.setAlignment(Pos.CENTER);

        Button newGameButton = new Button("New Game");
        newGameButton.setStyle("-fx-background-color: #3498db; -fx-text-fill: white; -fx-font-weight: bold;");
        newGameButton.setOnAction(e -> startNewGame());

        Button exitButton = new Button("Exit");
        exitButton.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white; -fx-font-weight: bold;");
        exitButton.setOnAction(e -> {
            cleanup();
            Platform.exit();
            System.exit(0);
        });

        controlPanel.getChildren().addAll(newGameButton, exitButton);
        return controlPanel;
    }

    private void handleCellClick(int row, int col) {
        if (gameState.isGameOver()) {
            showAlert("Game Over", "The game has ended. Start a new game to play again.");
            return;
        }

        // Check if it's a human player's turn
        boolean isCurrentPlayerHuman = (gameState.getCurrentPlayer().getColor() == COLOR.RED && player1Type == GameState.PlayerType.HUMAN) ||
                (gameState.getCurrentPlayer().getColor() == COLOR.BLUE && player2Type == GameState.PlayerType.HUMAN);

        if (!isCurrentPlayerHuman) {
            showAlert("AI Turn", "It's the AI's turn. Please wait...");
            return;
        }

        // Prevent rapid clicks
        if (System.currentTimeMillis() - lastMoveTime < 500) {
            return;
        }

        Cell clickedCell = gameState.getBoard()[row][col];

        if (!clickedCell.canTakeAction(gameState.getCurrentPlayer())) {
            showAlert("Invalid Move", "You can only place orbs in empty cells or cells with your color!");
            return;
        }

        // Make the move
        boolean success = gameState.takeAction(clickedCell);

        if (success) {
            lastMoveTime = System.currentTimeMillis();
            lastFileContent = gameState.toString();

            // Write the new state to file
            FileProtocol.writeGameState(gameState);

            updateBoard();
            updateCurrentPlayerLabel();
            checkGameOver();

            // For human vs human, add a small delay to prevent too fast switching
//            if (player1Type == GameState.PlayerType.HUMAN && player2Type == GameState.PlayerType.HUMAN) {
//                // Disable board temporarily
//                setBoardEnabled(false);
//                Platform.runLater(() -> {
//                    try {
//                        Thread.sleep(500);
//                    } catch (InterruptedException ie) {
//                        // Ignore
//                    }
//                    setBoardEnabled(true);
//                });
//            }
        }
    }

    private void setBoardEnabled(boolean enabled) {
        for (int row = 0; row < ROWS; row++) {
            for (int col = 0; col < COLS; col++) {
                cellButtons[row][col].setDisable(!enabled);
            }
        }
    }

    private void checkGameOver() {
        if (gameState.isGameOver()) {
            COLOR winner = gameState.getWinner();
            String winnerName = winner == COLOR.RED ? "Red Player" : "Blue Player";
            gameStatusLabel.setText("Game Over - " + winnerName + " Wins!");
            gameStatusLabel.setTextFill(winner == COLOR.RED ? Color.RED : Color.BLUE);

            showAlert("Game Over", winnerName + " wins the game!");
        }
    }

    private void updateBoard() {
        Cell[][] board = gameState.getBoard();

        for (int row = 0; row < ROWS; row++) {
            for (int col = 0; col < COLS; col++) {
                Cell cell = board[row][col];
                Button button = cellButtons[row][col];
                updateCellButton(button, cell);
            }
        }
    }

    private void updateCellButton(Button button, Cell cell) {
        if (cell.isEmpty()) {
            button.setText("");
            button.setGraphic(null);
            button.setStyle("-fx-background-color: #ecf0f1; -fx-border-color: #bdc3c7; -fx-border-width: 2;");
        } else {
            // Create visual representation with orbs
            button.setText(String.valueOf(cell.getOrbCount()));

            String colorStyle = cell.getOrbColor() == COLOR.RED ?
                    "-fx-background-color: #e74c3c; -fx-text-fill: white;" :
                    "-fx-background-color: #3498db; -fx-text-fill: white;";

            button.setStyle(colorStyle + " -fx-border-color: #2c3e50; -fx-border-width: 2; -fx-font-weight: bold; -fx-font-size: 16px;");
        }
    }

    private void updateCurrentPlayerLabel() {
        String playerType = "";
        if (gameState.getCurrentPlayer().getColor() == COLOR.RED) {
            playerType = player1Type == GameState.PlayerType.HUMAN ? " (Human)" : " (AI)";
            currentPlayerLabel.setText("Current Player: Red" + playerType);
            currentPlayerLabel.setTextFill(javafx.scene.paint.Color.valueOf("#e74c3c"));
        } else {
            playerType = player2Type == GameState.PlayerType.HUMAN ? " (Human)" : " (AI)";
            currentPlayerLabel.setText("Current Player: Blue" + playerType);
            currentPlayerLabel.setTextFill(javafx.scene.paint.Color.valueOf("#3498db"));
        }
    }

    private void startNewGame() {
        // Stop current processes and file watcher
        cleanup();

        // Show player selection dialog again
        PlayerSelectionDialog.PlayerConfiguration config = PlayerSelectionDialog.showDialog((Stage) boardGrid.getScene().getWindow());

        if (!config.cancelled) {
            player1Type = config.player1Type;
            player2Type = config.player2Type;

            // Create new game state
            gameState = new GameState();
            gameState.setPlayer1Type(player1Type);
            gameState.setPlayer2Type(player2Type);

            // Reset UI
            gameStatusLabel.setText("Game in Progress");
            gameStatusLabel.setTextFill(javafx.scene.paint.Color.LIGHTGRAY);

            // Reset tracking variables
            lastFileContent = "";
            isProcessingFileUpdate = false;
            lastMoveTime = 0;

            // Restart game
            initializeGame();
        }
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void cleanup() {
        if (fileWatcher != null) {
            fileWatcher.shutdown();
            try {
                if (!fileWatcher.awaitTermination(2, TimeUnit.SECONDS)) {
                    fileWatcher.shutdownNow();
                }
            } catch (InterruptedException e) {
                fileWatcher.shutdownNow();
            }
        }

        if (aiProcess1 != null && aiProcess1.isAlive()) {
            aiProcess1.destroyForcibly();
            try {
                aiProcess1.waitFor(2, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                // Ignore
            }
        }

        if (aiProcess2 != null && aiProcess2.isAlive()) {
            aiProcess2.destroyForcibly();
            try {
                aiProcess2.waitFor(2, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                // Ignore
            }
        }

        // Clean up the game state file
//        try {
//            java.nio.file.Files.deleteIfExists(java.nio.file.Paths.get("gamestate.txt"));
//        } catch (Exception e) {
//            // Ignore
//        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}