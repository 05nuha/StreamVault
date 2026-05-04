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

@WebServlet("/admin")
public class AdminServlet extends HttpServlet {
    protected void doGet(HttpServletRequest req, HttpServletResponse res)
            throws IOException {
        res.setContentType("text/html");
        PrintWriter out = res.getWriter();
        out.println("<html><head><title>Admin Analytics</title></head><body>");
        out.println("<h1>Admin Analytics Dashboard</h1>");

        String topSql = "SELECT ci.title, COUNT(w.history_id) AS watch_count " +
                        "FROM Content_Items ci " +
                        "LEFT JOIN Watch_History w ON ci.content_id = w.content_id " +
                        "GROUP BY ci.title ORDER BY watch_count DESC LIMIT 10";

        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(topSql)) {
            ResultSet rs = ps.executeQuery();
            out.println("<h2>Top 10 Most Watched Content</h2>");
            out.println("<table border='1'><tr><th>Title</th><th>Watch Count</th></tr>");
            while (rs.next()) {
                out.println("<tr><td>" + rs.getString("title") + "</td><td>" + rs.getInt("watch_count") + "</td></tr>");
            }
            out.println("</table>");
        } catch (Exception e) {
            out.println("<p>Error: " + e.getMessage() + "</p>");
        }

        String revSql = "SELECT p.plan_name, SUM(pay.amount) AS revenue " +
                        "FROM Payments pay " +
                        "JOIN Subscriptions s ON pay.subscription_id = s.subscription_id " +
                        "JOIN Subscription_Plans p ON s.plan_id = p.plan_id " +
                        "WHERE pay.status = 'completed' " +
                        "GROUP BY p.plan_name ORDER BY revenue DESC";

        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(revSql)) {
            ResultSet rs = ps.executeQuery();
            out.println("<h2>Revenue by Plan</h2>");
            out.println("<table border='1'><tr><th>Plan</th><th>Total Revenue</th></tr>");
            while (rs.next()) {
                out.println("<tr><td>" + rs.getString("plan_name") + "</td><td>$" + rs.getDouble("revenue") + "</td></tr>");
            }
            out.println("</table>");
        } catch (Exception e) {
            out.println("<p>Error: " + e.getMessage() + "</p>");
        }

        out.println("<br><a href='/home'>Back to Home</a>");
        out.println("</body></html>");
    }
}
