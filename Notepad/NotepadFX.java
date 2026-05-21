import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.*;
import java.nio.file.Files;
import java.time.LocalDateTime;



public class NotepadFX extends Application {

    private TextArea textArea = new TextArea();

    private Stage primaryStage;

    private File currentFile = null;

    private DocumentCounter counter = new DocumentCounter();

    @Override
    public void start(Stage stage) {

        this.primaryStage = stage;

        MenuBar menuBar = new MenuBar();

        Menu fileMenu = new Menu("File");

        Menu editMenu = new Menu("Edit");

        
        MenuItem newItem = new MenuItem("New");

        MenuItem openItem = new MenuItem("Open");

        MenuItem saveItem = new MenuItem("Save");

        MenuItem exitItem = new MenuItem("Exit");

        MenuItem wordCountItem = new MenuItem("Word Count");

        MenuItem clearItem = new MenuItem("Clear");

        fileMenu.getItems().addAll(
                newItem,
                openItem,
                saveItem,
                new SeparatorMenuItem(),
                exitItem
        );

        editMenu.getItems().addAll(
                wordCountItem,
                clearItem
        );

        menuBar.getMenus().addAll(fileMenu, editMenu);

         newItem.setOnAction(e -> {

            textArea.clear();

            currentFile = null;

            primaryStage.setTitle("New Document - NotepadFX");
        });

        openItem.setOnAction(e -> openFile());

        saveItem.setOnAction(e -> {
              new Thread(() -> saveFile()).start();
        });

        exitItem.setOnAction(e -> stage.close());

        clearItem.setOnAction(e -> textArea.clear());
        wordCountItem.setOnAction(e -> {

            counter.setText(textArea.getText());

            Alert alert = new Alert(Alert.AlertType.INFORMATION);

            alert.setTitle("Word Count");

            alert.setHeaderText(null);

            alert.setContentText(
                    "Words: " + counter.countWords()
                            + "\nCharacters: " + counter.countCharacters()
            );

            alert.showAndWait();
        });

        

        BorderPane root = new BorderPane();

        root.setTop(menuBar);

        root.setCenter(textArea);

        textArea.setWrapText(true);

        stage.setScene(new Scene(root, 600, 400));

        stage.setTitle("NotepadFX");

        stage.show();
    }

    

    private void openFile() {

        FileChooser fileChooser = new FileChooser();

        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Text Files", "*.txt")
        );

        File selectedFile = fileChooser.showOpenDialog(primaryStage);

        if (selectedFile != null) {

            try {

                String content = Files.readString(selectedFile.toPath());

                textArea.setText(content);

                currentFile = selectedFile;

                primaryStage.setTitle(currentFile.getName() + " - NotepadFX");

            } catch (IOException ex) {

                System.out.println("Error reading file");
            }
        }
    }


    private synchronized void saveFile() {

        if (currentFile == null) {

            FileChooser fileChooser = new FileChooser();

            fileChooser.getExtensionFilters().add(
                    new FileChooser.ExtensionFilter("Text Files", "*.txt")
            );

            currentFile = fileChooser.showSaveDialog(primaryStage);
        }

        if (currentFile != null) {

            try {

                
                String content =
                        textArea.getText()
                                + "\n\nSaved at: "
                                + LocalDateTime.now();

                Files.writeString(currentFile.toPath(), content);

                primaryStage.setTitle(
                        currentFile.getName() + " - NotepadFX"
                );

                System.out.println("File saved successfully");

            } catch (IOException ex) {

                System.out.println("Error saving file");
            }
        }
    }

    public static void main(String[] args) {

        launch(args);
    }
}



class DocumentCounter {

    private String text;

    public void setText(String text) {

        this.text = text;
    }

    public int countWords() {

        if (text == null || text.isEmpty()) {

            return 0;
        }

        String[] words = text.trim().split("\\s+");

        return words.length;
    }

    public int countCharacters() {

        if (text == null) {

            return 0;
        }

        return text.length();
    }
}