import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.net.*;
import java.io.*;
import java.nio.file.Files;
import java.time.LocalTime;
import java.util.Vector;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;

public class ServerFX extends Application {

    TextArea chatArea;
    TextField messageField;

    Button sendButton;
    Button fileButton;

    static Vector<ClientHandler> clients =
            new Vector<>();

    @Override
    public void start(Stage stage) {

        chatArea = new TextArea();

        chatArea.setEditable(false);

        messageField = new TextField();

        sendButton = new Button("Send");

        fileButton = new Button("Send File");

        HBox bottom = new HBox(
                10,
                messageField,
                sendButton,
                fileButton
        );

        VBox root =
                new VBox(10, chatArea, bottom);

        Scene scene =
                new Scene(root, 500, 400);

        stage.setScene(scene);

        stage.setTitle("Server");

        stage.show();

        loadChatHistory();

        sendButton.setOnAction(e -> sendMessage());

        messageField.setOnAction(e -> sendMessage());

        fileButton.setOnAction(e -> sendFile(stage));

        new Thread(this::startServer).start();
    }

    
    private void startServer() {

        try (ServerSocket server =
                     new ServerSocket(4405)) {

            append("Server started on port 4405...\n");

            while (true) {

                Socket socket =
                        server.accept();

                append("Client connected\n");

                ClientHandler client =
                        new ClientHandler(socket, this);

                clients.add(client);

                client.start();
            }

        } catch (Exception e) {

            append("Server stopped.\n");
        }
    }


    void append(String msg) {

        Platform.runLater(() ->
                chatArea.appendText(msg)
        );
    }

    public void saveMessage(
            String sender,
            String message,
            String time
    ) {

        try {

            Connection con =
                    DriverManager.getConnection(
                            "jdbc:mysql://localhost:3306/chatapp",
                            "root",
                            ""
                    );

            String sql =
                    "INSERT INTO messages(sender, message, time) VALUES(?,?,?)";

            PreparedStatement ps =
                    con.prepareStatement(sql);

            ps.setString(1, sender);

            ps.setString(2, message);

            ps.setString(3, time);

            ps.executeUpdate();

            con.close();

        } catch (Exception e) {

            System.out.println("DB Error");
        }
    }

    public void loadChatHistory() {

        try {

            Connection con =
                    DriverManager.getConnection(
                            "jdbc:mysql://localhost:3306/chatapp",
                            "root",
                            ""
                    );

            Statement st =
                    con.createStatement();

            ResultSet rs =
                    st.executeQuery(
                            "SELECT * FROM messages ORDER BY id DESC LIMIT 10"
                    );

            StringBuilder history =
                    new StringBuilder();

            while (rs.next()) {

                history.insert(
                        0,
                        "[" + rs.getString("time") + "] "
                                + rs.getString("sender")
                                + ": "
                                + rs.getString("message")
                                + "\n"
                );
            }

            chatArea.appendText(history.toString());

            con.close();

        } catch (Exception e) {

            chatArea.appendText(
                    "Failed to load chat history.\n"
            );
        }
    }
    private void sendMessage() {

        try {

            String text =
                    messageField.getText().trim();

            if (text.isEmpty()) {

                return;
            }

            String time =
                    LocalTime.now()
                            .withNano(0)
                            .toString();

            Message m =
                    new Message("SERVER", text);

            String msg =
                    "[" + time + "] "
                            + m.format();

            for (ClientHandler c : clients) {

                c.out.writeObject(msg);

                c.out.flush();
            }

            append(msg + "\n");

            saveMessage(
                    "SERVER",
                    text,
                    time
            );

            messageField.clear();

        } catch (Exception e) {

            append("Send failed\n");
        }
    }

    // ---------------- SEND FILE ----------------
    private void sendFile(Stage stage) {

        try {

            FileChooser chooser =
                    new FileChooser();

            chooser.setTitle("Choose File");

            File file =
                    chooser.showOpenDialog(stage);

            if (file == null) {

                return;
            }

            byte[] data =
                    Files.readAllBytes(
                            file.toPath()
                    );

            FileMessage fm =
                    new FileMessage(
                            file.getName(),
                            data
                    );

            for (ClientHandler c : clients) {

                c.out.writeObject(fm);

                c.out.flush();
            }

            append(
                    "Server sent file: "
                            + file.getName()
                            + "\n"
            );

        } catch (Exception e) {

            append("File send failed\n");
        }
    }

    public static void main(String[] args) {

        launch(args);
    }
}


class ClientHandler extends Thread {

    Socket socket;

    ObjectInputStream in;

    ObjectOutputStream out;

    ServerFX server;

    public ClientHandler(
            Socket socket,
            ServerFX server
    ) throws Exception {

        this.socket = socket;

        this.server = server;

        out =
                new ObjectOutputStream(
                        socket.getOutputStream()
                );

        out.flush();

        in =
                new ObjectInputStream(
                        socket.getInputStream()
                );
    }

    public void run() {

        try {

            while (true) {

                Object obj =
                        in.readObject();

                if (obj instanceof String msg) {

                    server.append(msg + "\n");

                    server.saveMessage(
                            "CLIENT",
                            msg,
                            LocalTime.now()
                                    .withNano(0)
                                    .toString()
                    );
                }

                else if (obj instanceof FileMessage fm) {

                    server.append(
                            "File received: "
                                    + fm.getFileName()
                                    + "\n"
                    );
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

                    server.append(
                            "File saved: received_files/"
                                    + fm.getFileName()
                                    + "\n"
                    );
                    for (ClientHandler c : ServerFX.clients) {

                        c.out.writeObject(fm);

                        c.out.flush();
                    }
                }
            }

        } catch (Exception e) {

            server.append("Client disconnected\n");

            try {

                socket.close();

            } catch (Exception ex) {

            }

            ServerFX.clients.remove(this);
        }
    }
}