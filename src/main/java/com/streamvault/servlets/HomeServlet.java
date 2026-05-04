package com.streamvault.servlets;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;

@WebServlet("/home")
public class HomeServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse res)
            throws ServletException, IOException {

        // check if user is logged in
        HttpSession session = req.getSession(false);
        if (session == null || session.getAttribute("user") == null) {
            res.sendRedirect("login.html");
            return;
        }

        String email = (String) session.getAttribute("user");

        // temporary placeholder - Kety will replace this with the real home page
        res.setContentType("text/html");
        res.getWriter().println("<!DOCTYPE html><html><head>");
        res.getWriter().println("<title>StreamVault</title>");
        res.getWriter().println("<style>body{background:#07081e;margin:0;}</style>");
        res.getWriter().println("</head><body></body></html>");
    }
}
