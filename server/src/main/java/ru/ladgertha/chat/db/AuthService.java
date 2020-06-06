package ru.ladgertha.chat.db;

public interface AuthService {
    String getNickname(String login, String password);

    void stop();

    boolean start();

    int getIdByNickname(String nickname);
}
