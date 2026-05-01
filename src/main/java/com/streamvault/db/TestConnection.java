package com.streamvault.db;

import java.sql.Connection;

// quick test to make sure the database connection works
public class TestConnection {

    public static void main(String[] args) throws Exception {
        Connection c = DatabaseConnection.getConnection();
        System.out.println("Connected: " + !c.isClosed());
        c.close();
    }
}
