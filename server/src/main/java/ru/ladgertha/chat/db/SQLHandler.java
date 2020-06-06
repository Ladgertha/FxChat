package ru.ladgertha.chat.db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class SQLHandler {
    private static Connection connection;

    private static Statement statement;

    public static void disconnect() {
        try {
            connection.close();
        } catch (SQLException exception) {
            exception.printStackTrace();
        }
        try {
            statement.close();
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }
    }

    public static void start() throws ClassNotFoundException, SQLException {
        Class.forName("org.sqlite.JDBC");
        connection = DriverManager.getConnection("jdbc:sqlite:users.db");
        statement = connection.createStatement();
    }

    public static void addToHistory(int senderId, int receiverId, String message) {
        try {
            statement.executeUpdate(String.format("INSERT INTO message_history (sender_id, receiver_id, message) VALUES (%d, %d, '%s');",
                    senderId, receiverId, message));
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }
    }

    public static boolean changeNickname(int id, String newNickname) {
        try {
            statement.executeUpdate(String.format("UPDATE users SET nickname = '%s' WHERE ID = %d;", newNickname, id));
            return true;
        } catch (SQLException exception) {
            return false;
        }
    }

    public static Statement getStatement() {
        return statement;
    }
}
