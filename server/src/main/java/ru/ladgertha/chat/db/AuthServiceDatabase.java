package ru.ladgertha.chat.db;

import java.sql.*;

public class AuthServiceDatabase implements AuthService {

    @Override
    public String getNickname(String login, String password) {
        try {
            ResultSet resultSet = SQLHandler.getStatement().executeQuery(
                    String.format("SELECT nickname FROM users WHERE login = '%s' AND password = '%s';", login, password));
            if (!resultSet.next()) return null;
            return resultSet.getString("nickname");
        } catch (SQLException exception) {
            exception.printStackTrace();
        }
        return null;
    }

    @Override
    public void stop() {
        SQLHandler.disconnect();
    }

    @Override
    public boolean start() {
        try {
            SQLHandler.start();
            return true;
        } catch (SQLException | ClassNotFoundException exception) {
            return false;
        }
    }

    @Override
    public int getIdByNickname(String nickname) {
        try {
            ResultSet resultSet = SQLHandler.getStatement().executeQuery(
                    String.format("SELECT id FROM users WHERE nickname = '%s';", nickname));
            resultSet.next();
            return resultSet.getInt(1);
        } catch (SQLException exception) {
            exception.printStackTrace();
        }
        return -1;
    }
}
