package com.streamvault.servlets;

import com.streamvault.db.DatabaseConnection;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

@WebServlet("/home")
public class HomeServlet extends HttpServlet {
    protected void doGet(HttpServletRequest req, HttpServletResponse res)
            throws IOException {
        String type = req.getParameter("type");
        String sql = "SELECT * FROM Content_Items";
        if (type != null && !type.isEmpty()) {
            sql += " WHERE type = ?";
        }
        sql += " ORDER BY title ASC";

        res.setContentType("text/html");
        PrintWriter out = res.getWriter();
        out.println("<html><head><title>StreamVault Home</title></head><body>");
        out.println("<h1>StreamVault — Browse Content</h1>");
        out.println("<form method='get'>");
        out.println("<select name='type'><option value=''>All</option>");
        out.println("<option value='Movie'>Movie</option>");
        out.println("<option value='Series'>Series</option>");
        out.println("<option value='Music'>Music</option>");
        out.println("<option value='Book'>Book</option>");
        out.println("</select>");
        out.println("<button type='submit'>Filter</button></form>");
        out.println("<table border='1'><tr><th>Title</th><th>Type</th><th>Year</th><th>Rating</th></tr>");

        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            if (type != null && !type.isEmpty()) ps.setString(1, type);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                out.println("<tr>");
                out.println("<td><a href='/content?id=" + rs.getInt("content_id") + "'>" + rs.getString("title") + "</a></td>");
                out.println("<td>" + rs.getString("type") + "</td>");
                out.println("<td>" + rs.getInt("release_year") + "</td>");
                out.println("<td>" + rs.getString("age_rating") + "</td>");
                out.println("</tr>");
            }
        } catch (Exception e) {
            out.println("<tr><td colspan='4'>Error: " + e.getMessage() + "</td></tr>");
        }
        out.println("</table>");
        out.println("<br><a href='/dashboard'>My Dashboard</a> | <a href='/logout'>Logout</a>");
        out.println("</body></html>");
    }
}