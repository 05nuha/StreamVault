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

@WebServlet("/dashboard")
public class DashboardServlet extends HttpServlet {
    protected void doGet(HttpServletRequest req, HttpServletResponse res)
            throws IOException {
        String email = (String) req.getSession().getAttribute("user");
        res.setContentType("text/html");
        PrintWriter out = res.getWriter();
        out.println("<html><head><title>My Dashboard</title></head><body>");
        out.println("<h1>My Dashboard</h1>");

        if (email == null) {
            res.sendRedirect("/login.html");
            return;
        }

        String subSql = "SELECT s.status, s.start_date, s.end_date, " +
                        "p.plan_name, p.monthly_price " +
                        "FROM Subscriptions s " +
                        "JOIN Subscription_Plans p ON s.plan_id = p.plan_id " +
                        "JOIN Users u ON s.user_id = u.user_id " +
                        "WHERE u.email = ? AND s.status = 'active'";

        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(subSql)) {
            ps.setString(1, email);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                out.println("<h2>Subscription</h2>");
                out.println("<p>Plan: " + rs.getString("plan_name") + "</p>");
                out.println("<p>Price: $" + rs.getDouble("monthly_price") + "/month</p>");
                out.println("<p>Status: " + rs.getString("status") + "</p>");
                out.println("<p>Expires: " + rs.getString("end_date") + "</p>");
            } else {
                out.println("<p>No active subscription found.</p>");
            }
        } catch (Exception e) {
            out.println("<p>Error: " + e.getMessage() + "</p>");
        }

        String historySql = "SELECT ci.title, wh.watch_date, wh.progress_pct, wh.completed " +
                            "FROM Watch_History wh " +
                            "JOIN Content_Items ci ON wh.content_id = ci.content_id " +
                            "JOIN Profiles p ON wh.profile_id = p.profile_id " +
                            "JOIN Users u ON p.user_id = u.user_id " +
                            "WHERE u.email = ? ORDER BY wh.watch_date DESC";

        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(historySql)) {
            ps.setString(1, email);
            ResultSet rs = ps.executeQuery();
            out.println("<h2>Watch History</h2>");
            out.println("<table border='1'><tr><th>Title</th><th>Date</th><th>Progress</th><th>Completed</th></tr>");
            while (rs.next()) {
                out.println("<tr>");
                out.println("<td>" + rs.getString("title") + "</td>");
                out.println("<td>" + rs.getString("watch_date") + "</td>");
                out.println("<td>" + rs.getInt("progress_pct") + "%</td>");
                out.println("<td>" + (rs.getBoolean("completed") ? "Yes" : "No") + "</td>");
                out.println("</tr>");
            }
            out.println("</table>");
        } catch (Exception e) {
            out.println("<p>Error: " + e.getMessage() + "</p>");
        }

        out.println("<br><a href='/home'>Browse Content</a> | <a href='/logout'>Logout</a>");
        out.println("</body></html>");
    }
}