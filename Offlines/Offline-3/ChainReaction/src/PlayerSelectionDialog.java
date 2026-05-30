import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.stage.Modality;
import javafx.stage.Stage;

public class PlayerSelectionDialog {

    public static class PlayerConfiguration {

        public boolean cancelled = false;

        public PlayerConfiguration(GameState.PlayerType p1, GameState.PlayerType p2) {
            this.player1Type = p1;
            this.player2Type = p2;
        }
    }

    public static PlayerConfiguration showDialog(Stage parentStage) {
        Stage dialog = new Stage();
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.initOwner(parentStage);
        dialog.setTitle("Player Selection");
        dialog.setResizable(false);

        // Create main layout
        VBox mainLayout = new VBox(20);
        mainLayout.setAlignment(Pos.CENTER);
        mainLayout.setPadding(new Insets(30));
        mainLayout.setStyle("-fx-background-color: #2c3e50;");

        // Title
        Text title = new Text("Chain Reaction Game Setup");
        title.setFont(Font.font("Arial", FontWeight.BOLD, 20));
        title.setStyle("-fx-fill: white;");

        // Player selection grid
        GridPane selectionGrid = new GridPane();
        selectionGrid.setAlignment(Pos.CENTER);
        selectionGrid.setHgap(20);
        selectionGrid.setVgap(15);
        selectionGrid.setPadding(new Insets(20));
        selectionGrid.setStyle("-fx-background-color: #34495e; -fx-background-radius: 10;");

        // Player 1 selection
        Label player1Label = new Label("Player 1 (Red):");
        player1Label.setStyle("-fx-text-fill: #e74c3c; -fx-font-weight: bold; -fx-font-size: 14px;");

        ToggleGroup player1Group = new ToggleGroup();
        RadioButton player1Human = new RadioButton("Human");
        RadioButton player1AI = new RadioButton("AI");
        player1Human.setToggleGroup(player1Group);
        player1AI.setToggleGroup(player1Group);
        player1Human.setSelected(true); // Default selection
        player1Human.setStyle("-fx-text-fill: white;");
        player1AI.setStyle("-fx-text-fill: white;");

        // Player 2 selection
        Label player2Label = new Label("Player 2 (Blue):");
        player2Label.setStyle("-fx-text-fill: #3498db; -fx-font-weight: bold; -fx-font-size: 14px;");

        ToggleGroup player2Group = new ToggleGroup();
        RadioButton player2Human = new RadioButton("Human");
        RadioButton player2AI = new RadioButton("AI");
        player2Human.setToggleGroup(player2Group);
        player2AI.setToggleGroup(player2Group);
        player2AI.setSelected(true); // Default: Human vs AI
        player2Human.setStyle("-fx-text-fill: white;");
        player2AI.setStyle("-fx-text-fill: white;");

        // Add to grid
        selectionGrid.add(player1Label, 0, 0);
        selectionGrid.add(player1Human, 1, 0);
        selectionGrid.add(player1AI, 2, 0);
        selectionGrid.add(player2Label, 0, 1);
        selectionGrid.add(player2Human, 1, 1);
        selectionGrid.add(player2AI, 2, 1);

        // Buttons
        HBox buttonBox = new HBox(15);
        buttonBox.setAlignment(Pos.CENTER);

        Button startButton = new Button("Start Game");
        startButton.setStyle("-fx-background-color: #27ae60; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 10 20;");

        Button cancelButton = new Button("Cancel");
        cancelButton.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 10 20;");

        buttonBox.getChildren().addAll(startButton, cancelButton);

        // Add all to main layout
        mainLayout.getChildren().addAll(title, selectionGrid, buttonBox);

        // Result holder
        PlayerConfiguration[] result = new PlayerConfiguration[1];
        result[0] = new PlayerConfiguration(GameState.PlayerType.HUMAN, GameState.PlayerType.AI);
        result[0].cancelled = true; // Default to cancelled

        // Button actions
        startButton.setOnAction(e -> {
            GameState.PlayerType p1Type = player1Human.isSelected() ?
                    GameState.PlayerType.HUMAN : GameState.PlayerType.AI;
            GameState.PlayerType p2Type = player2Human.isSelected() ?
                    GameState.PlayerType.HUMAN : GameState.PlayerType.AI;

            result[0] = new PlayerConfiguration(p1Type, p2Type);
            result[0].cancelled = false;
            dialog.close();
        });

        cancelButton.setOnAction(e -> {
            result[0].cancelled = true;
            dialog.close();
        });

        // Create and show scene
        Scene scene = new Scene(mainLayout, 400, 300);
        dialog.setScene(scene);
        dialog.showAndWait();

        return result[0];
    }
}