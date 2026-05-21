import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.*;
import java.net.*;
import java.nio.file.Files;
import java.time.LocalTime;

public class ClientFX extends Application {

    TextArea chatArea;
    TextField messageField;

    Button sendButton;
    Button fileButton;

    ObjectOutputStream out;

    Stage mainStage;

    @Override
    public void start(Stage stage) {

        mainStage = stage;

        chatArea = new TextArea();
        chatArea.setEditable(false);

        messageField = new TextField();
        messageField.setPromptText("Type message...");

        sendButton = new Button("Send");

        fileButton = new Button("Send File");

        HBox bottom = new HBox(
                10,
                messageField,
                sendButton,
                fileButton
        );

        VBox root = new VBox(10, chatArea, bottom);

        root.setStyle("-fx-padding: 10;");

        stage.setScene(new Scene(root, 500, 400));

        stage.setTitle("Chat Client");

        stage.show();

        sendButton.setOnAction(e -> sendMessage());

        messageField.setOnAction(e -> sendMessage());

        fileButton.setOnAction(e -> sendFile());

        new Thread(this::connectToServer).start();
    }

    private void connectToServer() {

        try {

            Socket socket = new Socket("localhost", 4405);

            out = new ObjectOutputStream(
                    socket.getOutputStream()
            );

            out.flush();

            ObjectInputStream in =
                    new ObjectInputStream(
                            socket.getInputStream()
                    );

            append("Connected to Server!\n");

            while (true) {

                Object obj = in.readObject();

                if (obj instanceof String msg) {

                    append(msg + "\n");
                }

                else if (obj instanceof FileMessage fm) {

                    File folder =
                            new File("received_files");

                    if (!folder.exists()) {

                        folder.mkdir();
                    }

                    FileOutputStream fos =
                            new FileOutputStream(
                                    "received_files/"
                                            + fm.getFileName()
                            );

                    fos.write(fm.getData());

                    fos.close();

                    append(
                            "Received file: "
                                    + fm.getFileName()
                                    + "\n"
                    );
                }
            }

        } catch (Exception e) {

            append("Disconnected from server.\n");
        }
    }

    private void sendMessage() {

        try {

            String text = messageField.getText().trim();

            if (text.isEmpty()) {

                return;
            }

            String time =
                    LocalTime.now()
                            .withNano(0)
                            .toString();

            Message m =
                    new Message("CLIENT", text);

            String msg =
                    "[" + time + "] "
                            + m.format();

            out.writeObject(msg);

            out.flush();

            messageField.clear();

        } catch (Exception e) {

            append("Failed to send message.\n");
        }
    }

    private void sendFile() {

        try {

            FileChooser chooser =
                    new FileChooser();

            chooser.setTitle("Choose File");

            File file =
                    chooser.showOpenDialog(mainStage);

            if (file == null) {

                return;
            }

            byte[] data =
                    Files.readAllBytes(file.toPath());

            FileMessage fm =
                    new FileMessage(
                            file.getName(),
                            data
                    );

            out.writeObject(fm);

            out.flush();

            append(
                    "You sent file: "
                            + file.getName()
                            + "\n"
            );

        } catch (Exception e) {

            append("File send failed\n");
        }
    }

    void append(String text) {

        Platform.runLater(() ->
                chatArea.appendText(text)
        );
    }

    public static void main(String[] args) {

        launch(args);
    }
}