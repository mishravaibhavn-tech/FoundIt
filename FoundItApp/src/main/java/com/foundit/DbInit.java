package com.foundit;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;

@WebListener
public class DbInit implements ServletContextListener {

    @Override
    public void contextInitialized(ServletContextEvent sce) {

        // 1. Create the database if this is the first run.
        try (Connection con = DBConnection.getServerConnection();
             Statement st = con.createStatement()) {

            st.executeUpdate("CREATE DATABASE IF NOT EXISTS " + DBConnection.dbName()
                           + " CHARACTER SET utf8mb4");
            log("database ready");

        } catch (SQLException e) {
            log("COULD NOT REACH MYSQL — is the server running? is the password right?");
            e.printStackTrace();
            return;
        }

        // 2. Create the three tables.
        try (Connection con = DBConnection.getConnection();
             Statement st = con.createStatement()) {

            st.executeUpdate(
                "CREATE TABLE IF NOT EXISTS users ("
              + " user_id  INT AUTO_INCREMENT PRIMARY KEY,"
              + " name     VARCHAR(100) NOT NULL,"
              + " email    VARCHAR(150) NOT NULL UNIQUE,"
              + " password VARCHAR(255) NOT NULL"
              + ")");

            st.executeUpdate(
                "CREATE TABLE IF NOT EXISTS items ("
              + " item_id         INT AUTO_INCREMENT PRIMARY KEY,"
              + " user_id         INT NOT NULL,"
              + " item_type       VARCHAR(10)  NOT NULL,"
              + " title           VARCHAR(120) NOT NULL,"
              + " description     TEXT,"
              + " location        VARCHAR(150),"
              + " secret_question VARCHAR(255) NOT NULL,"
              + " status          VARCHAR(20) DEFAULT 'OPEN',"
              + " created_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP,"
              + " FOREIGN KEY (user_id) REFERENCES users(user_id)"
              + ")");

            st.executeUpdate(
                "CREATE TABLE IF NOT EXISTS claims ("
              + " claim_id    INT AUTO_INCREMENT PRIMARY KEY,"
              + " item_id     INT NOT NULL,"
              + " claimant_id INT NOT NULL,"
              + " answer      TEXT NOT NULL,"
              + " status      VARCHAR(20) DEFAULT 'PENDING',"
              + " created_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP,"
              + " FOREIGN KEY (item_id)     REFERENCES items(item_id),"
              + " FOREIGN KEY (claimant_id) REFERENCES users(user_id),"
              + " UNIQUE KEY uq_claim (item_id, claimant_id)"
              + ")");

            log("tables ready — FoundIt started");

        } catch (SQLException e) {
            log("table creation failed");
            e.printStackTrace();
        }
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        log("FoundIt stopped");
    }

    private void log(String m) {
        System.out.println("[FoundIt] " + m);
    }
}