package ru.ladgertha.chat;

import ru.ladgertha.chat.db.AuthService;
import ru.ladgertha.chat.db.AuthServiceDatabase;
import ru.ladgertha.chat.db.SQLHandler;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Vector;

public class Server {
    private Vector<ClientHandler> clients;
    private AuthService authService;

    public Server() {
        authService = new AuthServiceDatabase();
        if (!authService.start()) {
            System.out.println("Что-то пошло не так при запуске сервиса авторизации");
            System.exit(0);
        }

        try (ServerSocket serverSocket = new ServerSocket(8189)) {
            clients = new Vector<>();
            System.out.println("Сервер запущен. Ожидаем подключение клиента");
            while (true) {
                Socket socket = serverSocket.accept();
                System.out.println("Клиент подключился");
                new ClientHandler(this, socket);
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            authService.stop();
        }
    }

    public void subscribe(ClientHandler clientHandler) {
        clients.add(clientHandler);
        broadcastOnlineClientsList();
    }

    public void unsubscribe(ClientHandler clientHandler) {
        clients.remove(clientHandler);
        broadcastOnlineClientsList();
    }

    public void broadcastMsg(String message) {
        for (ClientHandler clientHandler : clients) {
            clientHandler.sendMessage(message);
        }
    }

    public void sendPrivateMessage(ClientHandler clientHandler, String message, String nickname) {
        for (ClientHandler client : clients) {
            if (client.getNickname().equalsIgnoreCase(nickname)) {
                client.sendMessage("Приватное сообщение от " +
                        clientHandler.getNickname() + ": " + message);
                clientHandler.sendMessage("Приватное сообщение для " +
                        nickname + ": " + message);
                SQLHandler.addToHistory(clientHandler.getId(), client.getId(), message);
                return;
            }
        }
        clientHandler.sendMessage("Пользователь " +
                clientHandler.getNickname() + " не найден онлайн. Сообщение не отправлено.");
    }

    public AuthService getAuthService() {
        return authService;
    }

    public boolean isNicknameFree(String nickname) {
        for (ClientHandler clientHandler : clients) {
            if (clientHandler.getNickname().equals(nickname)) return false;
        }
        return true;
    }

    public void broadcastOnlineClientsList() {
        StringBuilder clientsList = new StringBuilder();
        clientsList.append("/onlineClients ");
        for (ClientHandler clientHandler : clients) {
            clientsList.append(clientHandler.getNickname()).append(" ");
        }
        clientsList.setLength(clientsList.length() - 1);
        String out = clientsList.toString();
        for (ClientHandler clientHandler : clients) {
            clientHandler.sendMessage(out);
        }
    }
}

