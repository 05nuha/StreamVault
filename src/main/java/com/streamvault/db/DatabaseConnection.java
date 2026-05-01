package com.streamvault.db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

// This class handles the connection to our MySQL database
public class DatabaseConnection {

    // database connection details
    private static final String URL = "jdbc:mysql://localhost:3306/streamvault";
    private static final String USER = "root";
    private static final String PASS = "yourpassword"; // change this to your MySQL password

    // returns a connection to the streamvault database
    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL, USER, PASS);
    }
}
