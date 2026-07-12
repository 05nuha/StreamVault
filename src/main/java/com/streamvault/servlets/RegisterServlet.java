package com.streamvault.servlets;

import com.streamvault.services.AuthService;
import com.streamvault.services.ValidationUtil;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

@WebServlet("/register")
public class RegisterServlet extends HttpServlet {

    // shows the registration page on GET
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse res)
            throws ServletException, IOException {
        req.getRequestDispatcher("/register.html").forward(req, res);
    }

    // processes the registration form on POST
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse res)
            throws ServletException, IOException {

        String name     = req.getParameter("name");
        String email    = req.getParameter("email");
        String password = req.getParameter("password");
        String country  = req.getParameter("country");
        String ctx      = req.getContextPath();

        int planId;
        try {
            planId = Integer.parseInt(req.getParameter("plan"));
        } catch (NumberFormatException e) {
            planId = 1; // default plan if none was provided
        }

        // validate before touching the database so the user gets a clear error
        if (!ValidationUtil.isValidName(name)
                || !ValidationUtil.isValidEmail(email)
                || !ValidationUtil.isValidPassword(password)) {
            res.sendRedirect(ctx + "/register.html?error=invalid");
            return;
        }

        if (AuthService.register(name, email, password, country, planId)) {
            res.sendRedirect(ctx + "/login.html?registered=1");
        } else {
            // most commonly the email is already registered
            res.sendRedirect(ctx + "/register.html?error=taken");
        }
    }
}
