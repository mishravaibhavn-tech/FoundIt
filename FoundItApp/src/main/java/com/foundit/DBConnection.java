package com.foundit;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DBConnection {

    private static final String HOST = "jdbc:mysql://localhost:3306/";
    private static final String DB   = "foundit_db";
    private static final String OPTS =
        "?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC";

    public static final String USER = "root";
    public static final String PASS = "";

    /** Connection to the FoundIt database. */
    public static Connection getConnection() throws SQLException {
        loadDriver();
        return DriverManager.getConnection(HOST + DB + OPTS, USER, PASS);
    }

    /** Connection to the MySQL server itself, with no database selected. */
    public static Connection getServerConnection() throws SQLException {
        loadDriver();
        return DriverManager.getConnection(HOST + OPTS, USER, PASS);
    }

    public static String dbName() { return DB; }

    private static void loadDriver() throws SQLException {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            throw new SQLException("MySQL driver missing from WEB-INF/lib", e);
        }
    }

    /** Escapes a string so it is safe inside a JSON value. */
    public static String esc(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "")
                .replace("\t", " ");
    }
}