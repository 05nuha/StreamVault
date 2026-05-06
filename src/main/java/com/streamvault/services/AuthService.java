package com.streamvault.services;

import com.streamvault.db.DatabaseConnection;
import org.mindrot.jbcrypt.BCrypt;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

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

    // registers a new user into Users and Subscriptions tables
    // password is hashed using BCrypt before storing
    public static void register(String name, String email, String pw, String country, int planId) {
        // hash the password with cost factor 12 before storing
        String hash = BCrypt.hashpw(pw, BCrypt.gensalt(12));

        String insertUser = "INSERT INTO Users(full_name, email, password, country) VALUES(?, ?, ?, ?)";
        String insertSub  = "INSERT INTO Subscriptions(user_id, plan_id, status, start_date, auto_renew) VALUES(?, ?, 'active', CURRENT_DATE, TRUE)";

        try (Connection c = DatabaseConnection.getConnection()) {

            // insert the user and get the auto-generated user_id back
            PreparedStatement psUser = c.prepareStatement(insertUser, Statement.RETURN_GENERATED_KEYS);
            psUser.setString(1, name);
            psUser.setString(2, email);
            psUser.setString(3, hash);
            psUser.setString(4, country);
            psUser.executeUpdate();

            // grab the new user_id so we can link the subscription to it
            ResultSet keys = psUser.getGeneratedKeys();
            if (keys.next()) {
                int userId = keys.getInt(1);

                // insert the subscription for the chosen plan
                PreparedStatement psSub = c.prepareStatement(insertSub);
                psSub.setInt(1, userId);
                psSub.setInt(2, planId);
                psSub.executeUpdate();

                System.out.println("User registered: " + email + " with plan " + planId);
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
