package com.streamvault.db;

import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

// reads db.url, db.user, db.password from db.properties and opens connections
public class DatabaseConnection {

    private static final String URL;
    private static final String USER;
    private static final String PASS;

    // runs once when the class is first used, loads the properties file
    static {
        try (InputStream in = DatabaseConnection.class.getClassLoader().getResourceAsStream("db.properties")) {
            if (in == null) {
                throw new RuntimeException("Could not find db.properties on the classpath");
            }
            Properties props = new Properties();
            props.load(in);
            URL = props.getProperty("db.url");
            USER = props.getProperty("db.user");
            PASS = props.getProperty("db.password");
        } catch (Exception e) {
            throw new RuntimeException("Could not load db.properties", e);
        }
    }

    public static Connection getConnection() throws SQLException {
        try {
            // without this Tomcat cant find the MySQL driver at runtime
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            throw new SQLException("MySQL driver not found", e);
        }
        return DriverManager.getConnection(URL, USER, PASS);
    }
}
