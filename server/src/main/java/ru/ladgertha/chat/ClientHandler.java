package ru.ladgertha.chat;

import ru.ladgertha.chat.db.SQLHandler;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;

public class ClientHandler {
    private int id;
    private Server server;
    private Socket socket;
    private DataInputStream in;
    private DataOutputStream out;
    private String nickname;

    public ClientHandler(Server server, Socket socket) {
        try {
            this.server = server;
            this.socket = socket;
            this.in = new DataInputStream(socket.getInputStream());
            this.out = new DataOutputStream(socket.getOutputStream());
            new Thread(() -> {
                try {
                    while (true) {
                        if (checkAuthorizationMessage(in.readUTF())) {
                            break;
                        }
                    }
                    while (true) {
                        String message = in.readUTF();

                        System.out.println("Сообщение от клиента: " + message);
                        if (message.startsWith("/")) {
                            if (message.equals("/end")) {
                                break;
                            }
                            handleCommandMessage(message);
                        } else {
                            SQLHandler.addToHistory(id, -1, message);
                            server.broadcastMsg(nickname + ": " + message);
                        }
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    try {
                        out.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    try {
                        in.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    try {
                        socket.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    server.unsubscribe(this);
                }
            }).start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void handleCommandMessage(String message) {
        if (message.startsWith("/w ")) {
            String[] tokens = message.split("\\s", 3);
            server.sendPrivateMessage(this, tokens[2], tokens[1]);
        }
        if (message.startsWith("/changeNickname ")) {
            String[] tokens = message.split("\\s");
            if (tokens.length == 2) {
                String newNickname = tokens[1];
                if (SQLHandler.changeNickname(id, newNickname)) {
                    sendMessage("Сообщение от сервера: Ваш новый ник " + newNickname);
                    sendMessage("/changeNicknameOk " + newNickname);
                    nickname = newNickname;
                    server.broadcastOnlineClientsList();
                } else {
                    sendMessage("Сообщение от сервера: поменять ник не удалось.");
                }
            }
        }
    }

    public boolean checkAuthorizationMessage(String message) {
        if (message.startsWith("/auth ")) {
            // /auth login password
            String[] tokens = message.split("\\s");
            if (tokens.length == 3) {
                nickname = server.getAuthService().getNickname(tokens[1], tokens[2]);
                if (nickname != null) {
                    if (server.isNicknameFree(nickname)) {
                        id = server.getAuthService().getIdByNickname(nickname);
                        sendMessage("/authsuccess " + nickname);
                        server.subscribe(this);
                        return true;
                    } else {
                        sendMessage("Учетная запись в данный момент используется.");
                    }
                } else {
                    sendMessage("Неверный логин или пароль.");
                }
            }
        }
        return false;
    }

    public void sendMessage(String message) {
        try {
            out.writeUTF(message);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public String getNickname() {
        return nickname;
    }

    public int getId() {
        return id;
    }
}

