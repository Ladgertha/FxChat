package ru.ladgertha.chat;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.net.URL;
import java.util.ResourceBundle;

public class Controller implements Initializable {
    private Socket socket;
    private DataInputStream in;
    private DataOutputStream out;
    private boolean authorized;
    private String currentNickname;

    @FXML
    TextField messageField, loginField;

    @FXML
    TextArea chatArea;

    @FXML
    HBox authPanel, messagePanel;

    @FXML
    PasswordField passwordField;

    @FXML
    ListView<String> clientsOnlineList;

    public void setAuthorized(boolean authorized) {
        this.authorized = authorized;
        setVisibleForPanels(authorized);
        if (!authorized) {
            currentNickname = null;
        }
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        setAuthorized(false);
        ObservableList<String> clientsList = FXCollections.observableArrayList();
        clientsOnlineList.setItems(clientsList);
        clientsOnlineList.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2) {
                messageField.clear();
                messageField.appendText("/w ");
                messageField.appendText(clientsOnlineList.getSelectionModel().getSelectedItem());
                messageField.appendText(" ");
                messageField.requestFocus();
                messageField.end();
            }
        });
    }

    public void connect() {
        try {
            socket = new Socket("localhost", 8189);
            in = new DataInputStream(socket.getInputStream());
            out = new DataOutputStream(socket.getOutputStream());

            Thread tRead = new Thread(() -> {
                try {
                    while (true) {
                        String authAnswer = in.readUTF();
                        if (authAnswer.startsWith("/authsuccess ")) {
                            chatArea.appendText("Сообщение от сервера: Ваш ник " + authAnswer.split("\\s")[1] + "\n");
                            currentNickname = authAnswer.split("\\s")[1];
                            setAuthorized(true);
                            break;
                        }
                        chatArea.appendText(authAnswer + "\n");
                    }
                    while (true) {
                        String serverMessage = in.readUTF();
                        if (serverMessage.startsWith("/")) {
                            if (serverMessage.startsWith("/onlineClients ")) {
                                String[] tokens = serverMessage.split("\\s");
                                Platform.runLater(() -> {
                                    clientsOnlineList.getItems().clear();
                                    for (int i = 1; i < tokens.length; i++) {
                                        clientsOnlineList.getItems().add(tokens[i]);
                                    }
                                });
                            }
                            if (serverMessage.startsWith("/changeNicknameOk ")) {
                                currentNickname = serverMessage.split("\\s")[1];
                            }
                        } else {
                            chatArea.appendText(serverMessage + "\n");
                        }
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    closeConnection();
                }
            });
            tRead.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void closeConnection() {
        try {
            in.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        try {
            out.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        try {
            socket.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        setAuthorized(false);
    }

    public void sendMessage() {
        try {
            out.writeUTF(messageField.getText());
            messageField.clear();
            messageField.requestFocus();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void sendAuthMessage() {
        if (socket == null || socket.isClosed()) {
            connect();
        }
        try {
            out.writeUTF("/auth " + loginField.getText() + " " + passwordField.getText());
            loginField.clear();
            passwordField.clear();
        } catch (IOException exception) {
            exception.printStackTrace();
        }
    }

    private void setVisibleForPanels(boolean authorized) {
        authPanel.setVisible(!authorized);
        authPanel.setManaged(!authorized);
        messagePanel.setVisible(authorized);
        messagePanel.setManaged(authorized);
    }
}
