package com.streamvault.db;

import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

// This class handles the connection to our MySQL database
public class DatabaseConnection {

    private static final String URL;
    private static final String USER;
    private static final String PASS;

    // load connection details from db.properties at class startup
    static {
        try {
            Properties props = new Properties();
            InputStream in = DatabaseConnection.class
                    .getClassLoader()
                    .getResourceAsStream("db.properties");
            props.load(in);
            URL  = props.getProperty("db.url");
            USER = props.getProperty("db.user");
            PASS = props.getProperty("db.password");
        } catch (Exception e) {
            throw new RuntimeException("Could not load db.properties", e);
        }
    }

    // returns a connection to the streamvault database
    public static Connection getConnection() throws SQLException {
        try {
            // explicitly load the MySQL driver - needed for Tomcat's class loader
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            throw new SQLException("MySQL driver not found", e);
        }
        return DriverManager.getConnection(URL, USER, PASS);
    }
}
