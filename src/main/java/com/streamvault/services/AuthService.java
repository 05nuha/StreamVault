package com.streamvault.services;

import com.streamvault.db.DatabaseConnection;
import org.mindrot.jbcrypt.BCrypt;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

// handles login and registration logic for users
public class AuthService {

    // checks if the email and password match a user in the database
    // returns true if login is successful, false otherwise
    public static boolean login(String email, String pw) {
        String sql = "SELECT password FROM Users WHERE email = ?";

        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {

            ps.setString(1, email);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                // use BCrypt to compare the entered password with the stored hash
                return BCrypt.checkpw(pw, rs.getString(1));
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return false;
    }

    // registers a new user into the Users table
    // password is hashed using BCrypt before storing
    public static void register(String name, String email, String pw, String country) {
        // hash the password with cost factor 12 before storing
        String hash = BCrypt.hashpw(pw, BCrypt.gensalt(12));

        String sql = "INSERT INTO Users(full_name, email, password, country) VALUES(?, ?, ?, ?)";

        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {

            ps.setString(1, name);
            ps.setString(2, email);
            ps.setString(3, hash);
            ps.setString(4, country);
            ps.executeUpdate();

            System.out.println("User registered: " + email);

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
