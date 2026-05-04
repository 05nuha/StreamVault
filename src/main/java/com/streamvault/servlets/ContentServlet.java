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

@WebServlet("/content")
public class ContentServlet extends HttpServlet {
    protected void doGet(HttpServletRequest req, HttpServletResponse res)
            throws IOException {
        String idParam = req.getParameter("id");
        res.setContentType("text/html");
        PrintWriter out = res.getWriter();
        out.println("<html><head><title>Content Detail</title></head><body>");

        if (idParam == null) {
            out.println("<p>No content selected.</p>");
            out.println("</body></html>");
            return;
        }

        int id = Integer.parseInt(idParam);
        String sql = "SELECT * FROM Content_Items WHERE content_id = ?";

        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, id);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                out.println("<h1>" + rs.getString("title") + "</h1>");
                out.println("<p>Type: " + rs.getString("type") + "</p>");
                out.println("<p>Year: " + rs.getInt("release_year") + "</p>");
                out.println("<p>Age Rating: " + rs.getString("age_rating") + "</p>");
                out.println("<p>Duration: " + rs.getInt("duration_minutes") + " mins</p>");
                out.println("<form method='post' action='/content?id=" + id + "'>");
                out.println("<button type='submit'>▶ Play Now</button>");
                out.println("</form>");
            } else {
                out.println("<p>Content not found.</p>");
            }
        } catch (Exception e) {
            out.println("<p>Error: " + e.getMessage() + "</p>");
        }
        out.println("<br><a href='/home'>Back to Browse</a>");
        out.println("</body></html>");
    }

    protected void doPost(HttpServletRequest req, HttpServletResponse res)
            throws IOException {
        String idParam = req.getParameter("id");
        String userEmail = (String) req.getSession().getAttribute("user");
        if (idParam != null && userEmail != null) {
            int contentId = Integer.parseInt(idParam);
            String sql = "INSERT INTO Watch_History (profile_id, content_id, watch_date, progress_pct, completed) " +
                         "SELECT p.profile_id, ?, CURDATE(), 0, FALSE " +
                         "FROM Profiles p JOIN Users u ON p.user_id = u.user_id " +
                         "WHERE u.email = ? LIMIT 1";
            try (Connection c = DatabaseConnection.getConnection();
                 PreparedStatement ps = c.prepareStatement(sql)) {
                ps.setInt(1, contentId);
                ps.setString(2, userEmail);
                ps.executeUpdate();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        res.sendRedirect("/content?id=" + idParam);
    }
}