
// ChainReactionUI.java (JavaFX UI)
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.stage.Stage;

import java.io.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;



public class ChainReactionUI extends Application {
    private static final String GAME_FILE = "gamestate.txt";
    private static final int CELL_SIZE = 80;

    private GameState gameState;
    private GridPane gameBoard;
    private Label statusLabel;
    private Button resetButton;
    private ScheduledExecutorService scheduler;

    @Override
    public void start(Stage primaryStage) {
        gameState = new GameState();

        // Create UI components
        createGameBoard();
        statusLabel = new Label("Red player's turn");
        statusLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");

        resetButton = new Button("New Game");
        resetButton.setOnAction(e -> resetGame());

        // Layout
        VBox root = new VBox(10);
        root.setAlignment(Pos.CENTER);
        root.setPadding(new Insets(20));
        root.getChildren().addAll(statusLabel, gameBoard, resetButton);

        Scene scene = new Scene(root, 600, 800);
        primaryStage.setTitle("Chain Reaction");
        primaryStage.setScene(scene);
        primaryStage.setOnCloseRequest(e -> {
            if (scheduler != null) scheduler.shutdown();
            Platform.exit();
        });
        primaryStage.show();

        // Start monitoring for AI moves
        startAIMonitoring();

        // Initialize game file
        try {
            gameState.saveToFile(GAME_FILE, false);
        } catch (IOException e) {
            showAlert("Error", "Could not create game file: " + e.getMessage());
        }
    }

    private void createGameBoard() {
        gameBoard = new GridPane();
        gameBoard.setAlignment(Pos.CENTER);
        gameBoard.setHgap(2);
        gameBoard.setVgap(2);

        for (int i = 0; i < GameState.ROWS; i++) {
            for (int j = 0; j < GameState.COLS; j++) {
                StackPane cellPane = createCellPane(i, j);
                gameBoard.add(cellPane, j, i);
            }
        }
    }

    private StackPane createCellPane(int row, int col) {
        StackPane cellPane = new StackPane();
        cellPane.setPrefSize(CELL_SIZE, CELL_SIZE);
        cellPane.setStyle("-fx-border-color: black; -fx-border-width: 1; -fx-background-color: lightgray;");

        cellPane.setOnMouseClicked(e -> handleCellClick(row, col));

        return cellPane;
    }

    private void handleCellClick(int row, int col) {
        if (gameState.isGameOver()) {
            showAlert("Game Over", "Game has ended. Winner: " + gameState.getWinner());
            return;
        }

        if (gameState.getCurrentPlayer() != GameState.RED) {
            showAlert("Not Your Turn", "Please wait for AI move");
            return;
        }

        if (!gameState.isValidMove(row, col)) {
            showAlert("Invalid Move", "You can only place orbs in empty cells or cells with your color");
            return;
        }

        // Make human move
        gameState.makeMove(row, col);
        updateDisplay();

        // Save to file
        try {
            gameState.saveToFile(GAME_FILE, true);
        } catch (IOException e) {
            showAlert("Error", "Could not save game state: " + e.getMessage());
        }

        if (gameState.isGameOver()) {
            statusLabel.setText("Game Over! Winner: " + gameState.getWinner());
            showAlert("Game Over", "Winner: " + gameState.getWinner());
        } else {
            statusLabel.setText("AI is thinking...");
        }
    }

    private void startAIMonitoring() {
        scheduler = Executors.newSingleThreadScheduledExecutor();
        scheduler.scheduleAtFixedRate(() -> {
            try {
                File file = new File(GAME_FILE);
                if (file.exists()) {
                    GameState newState = GameState.loadFromFile(GAME_FILE);

                    // Check if AI made a move
                    if (newState.getCurrentPlayer() == GameState.RED &&
                            !newState.isGameOver()) {
                        Platform.runLater(() -> {
                            gameState = newState;
                            updateDisplay();
                            statusLabel.setText("Your turn (Red)");
                        });
                    } else if (newState.isGameOver()) {
                        Platform.runLater(() -> {
                            gameState = newState;
                            updateDisplay();
                            statusLabel.setText("Game Over! Winner: " + gameState.getWinner());
                            showAlert("Game Over", "Winner: " + gameState.getWinner());
                        });
                    }
                }
            } catch (IOException e) {
                // Ignore file read errors (file might be being written)
            }
        }, 0, 500, TimeUnit.MILLISECONDS);
    }

    private void updateDisplay() {
        for (int i = 0; i < GameState.ROWS; i++) {
            for (int j = 0; j < GameState.COLS; j++) {
                StackPane cellPane = (StackPane) getNodeByRowCol(i, j);
                updateCellDisplay(cellPane, gameState.getCell(i, j));
            }
        }
    }

    private void updateCellDisplay(StackPane cellPane, GameState.Cell cell) {
        cellPane.getChildren().clear();

        if (cell.isEmpty()) {
            return;
        }

        Color orbColor = (cell.color == GameState.RED) ? Color.RED : Color.BLUE;

        if (cell.count == 1) {
            Circle orb = new Circle(15, orbColor);
            cellPane.getChildren().add(orb);
        } else if (cell.count == 2) {
            HBox orbContainer = new HBox(5);
            orbContainer.setAlignment(Pos.CENTER);
            for (int i = 0; i < 2; i++) {
                Circle orb = new Circle(12, orbColor);
                orbContainer.getChildren().add(orb);
            }
            cellPane.getChildren().add(orbContainer);
        } else if (cell.count == 3) {
            VBox orbContainer = new VBox(5);
            orbContainer.setAlignment(Pos.CENTER);
            Circle topOrb = new Circle(10, orbColor);
            HBox bottomOrbs = new HBox(5);
            bottomOrbs.setAlignment(Pos.CENTER);
            for (int i = 0; i < 2; i++) {
                Circle orb = new Circle(10, orbColor);
                bottomOrbs.getChildren().add(orb);
            }
            orbContainer.getChildren().addAll(topOrb, bottomOrbs);
            cellPane.getChildren().add(orbContainer);
        } else {
            // For 4+ orbs, show count as text
            Label countLabel = new Label(String.valueOf(cell.count));
            countLabel.setTextFill(orbColor);
            countLabel.setStyle("-fx-font-size: 20px; -fx-font-weight: bold;");
            cellPane.getChildren().add(countLabel);
        }
    }

    private javafx.scene.Node getNodeByRowCol(int row, int col) {
        for (javafx.scene.Node node : gameBoard.getChildren()) {
            Integer nodeCol = GridPane.getColumnIndex(node);
            Integer nodeRow = GridPane.getRowIndex(node);
            if (nodeRow != null && nodeCol != null && nodeRow == row && nodeCol == col) {
                return node;
            }
        }
        return null;
    }

    private void resetGame() {
        gameState = new GameState();
        updateDisplay();
        statusLabel.setText("Red player's turn");

        try {
            gameState.saveToFile(GAME_FILE, false);
        } catch (IOException e) {
            showAlert("Error", "Could not reset game file: " + e.getMessage());
        }
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
