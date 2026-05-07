package com.streamvault.servlets;

import com.streamvault.db.DatabaseConnection;
import com.streamvault.services.AuthService;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

@WebServlet("/login")
public class LoginServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse res)
            throws ServletException, IOException {
        req.getRequestDispatcher("/login.html").forward(req, res);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse res)
            throws ServletException, IOException {

        String email    = req.getParameter("email");
        String password = req.getParameter("password");
        String ctx      = req.getContextPath();

        if (AuthService.login(email, password)) {
            HttpSession session = req.getSession();
            session.setAttribute("user", email);

            // check role and redirect to the right page
            String role = fetchRole(email);
            session.setAttribute("role", role);

            if ("admin".equals(role)) {
                res.sendRedirect(ctx + "/admin");
            } else if ("content_manager".equals(role)) {
                res.sendRedirect(ctx + "/content-manager");
            } else {
                res.sendRedirect(ctx + "/home");
            }
        } else {
            res.sendRedirect(ctx + "/login.html?error=1");
        }
    }

    private String fetchRole(String email) {
        String sql = "SELECT role FROM Users WHERE email = ?";
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, email);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                String r = rs.getString("role");
                return r != null ? r : "viewer";
            }
        } catch (Exception ignored) {}
        return "viewer";
    }
}
