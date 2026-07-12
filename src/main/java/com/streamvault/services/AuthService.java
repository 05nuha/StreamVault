package com.streamvault.services;

import com.streamvault.db.DatabaseConnection;
import org.mindrot.jbcrypt.BCrypt;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.logging.Level;
import java.util.logging.Logger;

// handles login and registration logic for users
public class AuthService {

    private static final Logger LOG = Logger.getLogger(AuthService.class.getName());

    /**
     * Verifies the credentials and returns the user's role in one query.
     *
     * @return the role ("admin", "content_manager", "viewer", ...) when the
     *         credentials are valid, or null when they are not
     */
    public static String authenticate(String email, String pw) {
        if (email == null || pw == null) {
            return null;
        }
        String sql = "SELECT password, role FROM Users WHERE email = ?";

        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {

            ps.setString(1, email);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next() && BCrypt.checkpw(pw, rs.getString("password"))) {
                    String role = rs.getString("role");
                    return role != null ? role : "viewer";
                }
            }
        } catch (SQLException e) {
            LOG.log(Level.SEVERE, "Login query failed", e);
        }
        return null;
    }

    // kept for backward compatibility with callers that only need a yes/no
    public static boolean login(String email, String pw) {
        return authenticate(email, pw) != null;
    }

    /**
     * Registers a new user and their subscription atomically.
     * The password is hashed with BCrypt (cost 12) before storing.
     *
     * @return true when the user was created, false otherwise (e.g. the
     *         email is already taken or validation failed)
     */
    public static boolean register(String name, String email, String pw, String country, int planId) {
        if (!ValidationUtil.isValidName(name)
                || !ValidationUtil.isValidEmail(email)
                || !ValidationUtil.isValidPassword(pw)) {
            return false;
        }

        String hash = BCrypt.hashpw(pw, BCrypt.gensalt(12));

        String insertUser = "INSERT INTO Users(full_name, email, password, country) VALUES(?, ?, ?, ?)";
        String insertSub  = "INSERT INTO Subscriptions(user_id, plan_id, status, start_date, auto_renew) VALUES(?, ?, 'active', CURRENT_DATE, TRUE)";

        try (Connection c = DatabaseConnection.getConnection()) {
            // both inserts succeed or neither does — no orphaned users
            c.setAutoCommit(false);
            try (PreparedStatement psUser = c.prepareStatement(insertUser, Statement.RETURN_GENERATED_KEYS)) {
                psUser.setString(1, name.trim());
                psUser.setString(2, email.trim());
                psUser.setString(3, hash);
                psUser.setString(4, country);
                psUser.executeUpdate();

                try (ResultSet keys = psUser.getGeneratedKeys()) {
                    if (!keys.next()) {
                        c.rollback();
                        return false;
                    }
                    int userId = keys.getInt(1);
                    try (PreparedStatement psSub = c.prepareStatement(insertSub)) {
                        psSub.setInt(1, userId);
                        psSub.setInt(2, planId);
                        psSub.executeUpdate();
                    }
                }
                c.commit();
                LOG.info("User registered with plan " + planId);
                return true;
            } catch (SQLException e) {
                c.rollback();
                // duplicate email lands here via the unique constraint
                LOG.log(Level.WARNING, "Registration failed", e);
                return false;
            }
        } catch (SQLException e) {
            LOG.log(Level.SEVERE, "Registration connection failed", e);
            return false;
        }
    }
}
