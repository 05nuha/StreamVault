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

    // handles GET requests - just shows the login page
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse res)
            throws ServletException, IOException {
        req.getRequestDispatcher("/login.html").forward(req, res);
    }

    // handles POST requests - processes the login form
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse res)
            throws ServletException, IOException {

        String email = req.getParameter("email");
        String password = req.getParameter("password");

        // check credentials using AuthService
        if (AuthService.login(email, password)) {
            // login successful - store user email in session and redirect to home
            HttpSession session = req.getSession();
            session.setAttribute("user", email);
            res.sendRedirect("home");
        } else {
            // login failed - redirect back to login page with error message
            res.sendRedirect("login.html?error=1");
        }
    }
}
