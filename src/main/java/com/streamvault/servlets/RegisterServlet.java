package com.streamvault.servlets;

import com.streamvault.services.AuthService;
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

        // get all the form fields
        String name     = req.getParameter("name");
        String email    = req.getParameter("email");
        String password = req.getParameter("password");
        String country = req.getParameter("country");
        int planId;
        try {
            planId = Integer.parseInt(req.getParameter("plan"));
        } catch (NumberFormatException e) {
            planId = 1; // default plan if none was provided
        }

        // call AuthService to register the user and create their subscription
        AuthService.register(name, email, password, country, planId);

        // redirect to login page after successful registration
        res.sendRedirect(req.getContextPath() + "/login.html?registered=1");
    }
}
