package com.streamvault.servlets;

import com.streamvault.services.AuthService;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;

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

        // one query verifies the credentials and fetches the role
        String role = AuthService.authenticate(email, password);

        if (role != null) {
            // rotate the session id on login to prevent session fixation
            HttpSession oldSession = req.getSession(false);
            if (oldSession != null) {
                oldSession.invalidate();
            }
            HttpSession session = req.getSession(true);
            session.setAttribute("user", email);
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
}
