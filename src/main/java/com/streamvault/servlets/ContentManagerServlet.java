package com.streamvault.servlets;

import com.streamvault.db.DatabaseConnection;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

@WebServlet("/content-manager")
public class ContentManagerServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse res)
            throws ServletException, IOException {

        HttpSession session = req.getSession(false);
        if (session == null || session.getAttribute("user") == null) {
            res.sendRedirect(req.getContextPath() + "/login.html");
            return;
        }

        String role = (String) session.getAttribute("role");
        if (!"content_manager".equals(role) && !"admin".equals(role)) {
            res.sendRedirect(req.getContextPath() + "/home");
            return;
        }

        String email       = (String) session.getAttribute("user");
        String contextPath = req.getContextPath();
        res.setContentType("text/html");
        PrintWriter out = res.getWriter();

        out.println("<!DOCTYPE html><html lang='en'><head><meta charset='UTF-8'>");
        out.println("<meta name='viewport' content='width=device-width, initial-scale=1.0'>");
        out.println("<title>Content Manager — StreamVault</title>");
        out.println("<link rel='stylesheet' href='" + contextPath + "/css/style.css'>");
        out.println("</head><body class='page-shell'><div class='page-card'>");

        out.println("<header class='page-header'>");
        out.println("<div><h1>Content Manager</h1><p>Manage content items and monitor catalogue health.</p></div>");
        out.println("<nav class='nav-links'>");
        out.println("<a href='" + contextPath + "/home'>Browse</a>");
        out.println("<a href='" + contextPath + "/dashboard'>Dashboard</a>");
        out.println("<a href='" + contextPath + "/content-manager'>Content Manager</a>");
        if ("admin".equals(role)) out.println("<a href='" + contextPath + "/admin'>Admin</a>");
        out.println("<a href='" + contextPath + "/logout'>Logout</a>");
        out.println("</nav></header>");

        out.println("<div class='section-title'>Signed in as " + escapeHtml(email) + " &mdash; <span style='color:#a78bfa'>Content Manager</span></div>");

        // ── Catalogue summary cards ───────────────────────────────────────────
        out.println("<div class='card-grid'>");
        printStatCard(out, "Total Titles",  countQuery("SELECT COUNT(*) FROM Content_Items"), "&#127916;");
        printStatCard(out, "Movies",        countQuery("SELECT COUNT(*) FROM Content_Items WHERE type='Movie'"), "&#127910;");
        printStatCard(out, "Series",        countQuery("SELECT COUNT(*) FROM Content_Items WHERE type='Series'"), "&#128250;");
        printStatCard(out, "Total Reviews", countQuery("SELECT COUNT(*) FROM Reviews"), "&#11088;");
        out.println("</div>");

        // ── Full content catalogue table ──────────────────────────────────────
        out.println("<div class='section-title' style='margin-top:32px'>Content Catalogue</div>");
        out.println("<table class='content-table'><thead><tr>");
        out.println("<th>ID</th><th>Title</th><th>Type</th><th>Year</th><th>Rating</th><th>Reviews</th><th>Studio</th>");
        out.println("</tr></thead><tbody>");

        String catSql = "SELECT ci.content_id, ci.title, ci.type, ci.release_year, " +
                        "ci.average_rating, ci.total_reviews, s.studio_name " +
                        "FROM Content_Items ci LEFT JOIN Studios s ON ci.studio_id = s.studio_id " +
                        "ORDER BY ci.average_rating DESC";
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(catSql);
             ResultSet rs = ps.executeQuery()) {
            int count = 0;
            while (rs.next()) {
                count++;
                out.println("<tr>");
                out.println("<td>" + rs.getInt("content_id") + "</td>");
                out.println("<td><a href='" + contextPath + "/content?id=" + rs.getInt("content_id") +
                            "' style='color:#c4b5fd'>" + escapeHtml(rs.getString("title")) + "</a></td>");
                out.println("<td>" + escapeHtml(rs.getString("type")) + "</td>");
                out.println("<td>" + rs.getInt("release_year") + "</td>");
                out.println("<td>&#11088; " + rs.getBigDecimal("average_rating") + "</td>");
                out.println("<td>" + rs.getInt("total_reviews") + "</td>");
                out.println("<td>" + escapeHtml(rs.getString("studio_name")) + "</td>");
                out.println("</tr>");
            }
            if (count == 0) out.println("<tr><td colspan='7'>No content found.</td></tr>");
        } catch (Exception e) {
            out.println("<tr><td colspan='7'>Error: " + escapeHtml(e.getMessage()) + "</td></tr>");
        }
        out.println("</tbody></table>");

        // ── Recent Reviews ────────────────────────────────────────────────────
        out.println("<div class='section-title' style='margin-top:32px'>Recent Reviews</div>");
        out.println("<table class='content-table'><thead><tr>");
        out.println("<th>Title</th><th>User</th><th>Rating</th><th>Review</th><th>Date</th>");
        out.println("</tr></thead><tbody>");

        String revSql = "SELECT ci.title, u.full_name, r.rating, r.review_text, r.review_date " +
                        "FROM Reviews r " +
                        "JOIN Content_Items ci ON r.content_id = ci.content_id " +
                        "JOIN Users u ON r.user_id = u.user_id " +
                        "ORDER BY r.review_date DESC LIMIT 20";
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(revSql);
             ResultSet rs = ps.executeQuery()) {
            int count = 0;
            while (rs.next()) {
                count++;
                String preview = rs.getString("review_text");
                if (preview != null && preview.length() > 80) preview = preview.substring(0, 80) + "…";
                out.println("<tr>");
                out.println("<td>" + escapeHtml(rs.getString("title")) + "</td>");
                out.println("<td>" + escapeHtml(rs.getString("full_name")) + "</td>");
                out.println("<td>&#11088; " + rs.getBigDecimal("rating") + "</td>");
                out.println("<td style='max-width:280px'>" + escapeHtml(preview) + "</td>");
                out.println("<td>" + rs.getString("review_date") + "</td>");
                out.println("</tr>");
            }
            if (count == 0) out.println("<tr><td colspan='5'>No reviews yet.</td></tr>");
        } catch (Exception e) {
            out.println("<tr><td colspan='5'>Error: " + escapeHtml(e.getMessage()) + "</td></tr>");
        }
        out.println("</tbody></table>");

        out.println("<div style='margin-top:28px'>");
        out.println("<a class='btn-secondary' href='" + contextPath + "/home'>Back to Browse</a>");
        out.println("</div>");
        out.println("</div></body></html>");
    }

    private void printStatCard(PrintWriter out, String label, String value, String icon) {
        out.println("<div class='stat-card'><h3>" + icon + " " + label + "</h3><p style='font-size:1.8rem;font-weight:700;margin:8px 0 0'>" + value + "</p></div>");
    }

    private String countQuery(String sql) {
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) return String.valueOf(rs.getInt(1));
        } catch (Exception ignored) {}
        return "—";
    }

    private static String escapeHtml(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
    }
}
