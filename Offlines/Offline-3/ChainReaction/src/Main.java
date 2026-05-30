import javafx.application.Application;
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

public class Main extends Application {

    // Board dimensions (9 rows x 6 columns as per assignment)
    private static final int ROWS = 9;
    private static final int COLS = 6;
    private static final int CELL_SIZE = 80;

    private GameState gameState;
    private GridPane boardGrid;
    private Label currentPlayerLabel;
    private Label gameStatusLabel;
    private Button[][] cellButtons;

    @Override
    public void start(Stage primaryStage) {
        // Initialize game state
        gameState = new GameState();

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
        primaryStage.setTitle("Chain Reaction Game");
        primaryStage.setScene(scene);
        primaryStage.setResizable(false);
        primaryStage.show();

        // Update the display
        updateBoard();
    }

    private VBox createHeader() {
        VBox header = new VBox(10);
        header.setAlignment(Pos.CENTER);

        // Game title
        Text title = new Text("Chain Reaction");
        title.setFont(Font.font("Arial", FontWeight.BOLD, 24));
        title.setFill(Color.WHITE);

        // Current player label
        currentPlayerLabel = new Label();
        currentPlayerLabel.setFont(Font.font("Arial", FontWeight.BOLD, 16));
        updateCurrentPlayerLabel();

        // Game status label
        gameStatusLabel = new Label("Game in Progress");
        gameStatusLabel.setFont(Font.font("Arial", FontWeight.NORMAL, 14));
        gameStatusLabel.setTextFill(Color.LIGHTGRAY);

        header.getChildren().addAll(title, currentPlayerLabel, gameStatusLabel);
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
        exitButton.setOnAction(e -> System.exit(0));

        controlPanel.getChildren().addAll(newGameButton, exitButton);
        return controlPanel;
    }

    private void handleCellClick(int row, int col) {
        if (gameState.isGameOver()) {
            showAlert("Game Over", "The game has ended. Start a new game to play again.");
            return;
        }

        Cell clickedCell = gameState.getBoard()[row][col];

        if (!clickedCell.canTakeAction(gameState.getCurrentPlayer())) {
            showAlert("Invalid Move", "You can only place orbs in empty cells or cells with your color!");
            return;
        }

        // Make the move
        boolean success = gameState.takeAction(clickedCell);
        System.out.println("Clicked cell: (" + row + ", " + col + "), Success: " + success);
        // show orbcount and critical
        System.out.println("Cell Orb Count: " + clickedCell.getOrbCount() +
                ", Critical Mass: " + clickedCell.getCriticalMass());

        if (success) {
            updateBoard();
            updateCurrentPlayerLabel();

            // Check if game is over
            if (gameState.isGameOver()) {
                COLOR winner = gameState.getWinner();
                String winnerName = winner == COLOR.RED ? "Red Player" : "Blue Player";
                gameStatusLabel.setText("Game Over - " + winnerName + " Wins!");
                gameStatusLabel.setTextFill(winner == COLOR.RED ? Color.RED : Color.BLUE);

                showAlert("Game Over", winnerName + " wins the game!");
            }
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
            // Clear text and create visual representation with circles
            button.setText("");

            // Create a pane to hold the circles
            StackPane orbPane = new StackPane();
            orbPane.setPrefSize(CELL_SIZE - 10, CELL_SIZE - 10);

            // Set color based on orb color
            Color orbColor;
            String backgroundColor;

            if (cell.getOrbColor() == COLOR.RED) {
                orbColor = Color.WHITE;
                backgroundColor = "#e74c3c";
            } else if (cell.getOrbColor() == COLOR.BLUE) {
                orbColor = Color.WHITE;
                backgroundColor = "#3498db";
            } else {
                orbColor = Color.BLACK;
                backgroundColor = "#ecf0f1";
            }

            // Create circles based on orb count
            int orbCount = cell.getOrbCount();
            if (orbCount == 1) {
                // Single circle in center
                Circle orb = new Circle(8, orbColor);
                orbPane.getChildren().add(orb);
            } else if (orbCount == 2) {
                // Two circles horizontally
                HBox hbox = new HBox(5);
                hbox.setAlignment(Pos.CENTER);
                for (int i = 0; i < 2; i++) {
                    Circle orb = new Circle(6, orbColor);
                    hbox.getChildren().add(orb);
                }
                orbPane.getChildren().add(hbox);
            } else if (orbCount == 3) {
                // Three circles in triangle
                VBox vbox = new VBox(2);
                vbox.setAlignment(Pos.CENTER);

                // Top circle
                Circle topOrb = new Circle(5, orbColor);
                StackPane topPane = new StackPane(topOrb);

                // Bottom two circles
                HBox bottomBox = new HBox(3);
                bottomBox.setAlignment(Pos.CENTER);
                for (int i = 0; i < 2; i++) {
                    Circle orb = new Circle(5, orbColor);
                    bottomBox.getChildren().add(orb);
                }

                vbox.getChildren().addAll(topPane, bottomBox);
                orbPane.getChildren().add(vbox);
            } else {
                // For more than 3, show number with small circles
                VBox vbox = new VBox(2);
                vbox.setAlignment(Pos.CENTER);

                // Show count as text
                Text countText = new Text(String.valueOf(orbCount));
                countText.setFill(orbColor);
                countText.setFont(Font.font("Arial", FontWeight.BOLD, 12));

                // Add a small circle below
                Circle smallOrb = new Circle(4, orbColor);

                vbox.getChildren().addAll(countText, smallOrb);
                orbPane.getChildren().add(vbox);
            }

            button.setGraphic(orbPane);
            button.setStyle(String.format(
                    "-fx-background-color: %s; -fx-border-color: #34495e; -fx-border-width: 2;",
                    backgroundColor
            ));

            // Add visual indicator for cells close to critical mass
            if (cell.getOrbCount() >= cell.getCriticalMass() - 1) {
                button.setStyle(button.getStyle() + " -fx-border-color: #f39c12; -fx-border-width: 3;");
            }
        }
    }

    private void updateCurrentPlayerLabel() {
        COLOR currentColor = gameState.getCurrentPlayer().getColor();
        String playerName = currentColor == COLOR.RED ? "Red Player" : "Blue Player";

        currentPlayerLabel.setText("Current Turn: " + playerName);
        currentPlayerLabel.setTextFill(currentColor == COLOR.RED ? Color.RED : Color.BLUE);
    }

    private void startNewGame() {
        gameState = new GameState();
        updateBoard();
        updateCurrentPlayerLabel();
        gameStatusLabel.setText("Game in Progress");
        gameStatusLabel.setTextFill(Color.LIGHTGRAY);
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