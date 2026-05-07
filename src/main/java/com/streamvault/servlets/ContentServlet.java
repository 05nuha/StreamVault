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

@WebServlet("/content")
public class ContentServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse res)
            throws ServletException, IOException {

        HttpSession session = req.getSession(false);
        if (session == null || session.getAttribute("user") == null) {
            res.sendRedirect(req.getContextPath() + "/login.html");
            return;
        }

        String idParam     = req.getParameter("id");
        String role        = (String) session.getAttribute("role");
        String contextPath = req.getContextPath();
        res.setContentType("text/html");
        PrintWriter out = res.getWriter();

        out.println("<!DOCTYPE html><html lang='en'><head><meta charset='UTF-8'>");
        out.println("<meta name='viewport' content='width=device-width, initial-scale=1.0'>");
        out.println("<title>Content Detail — StreamVault</title>");
        out.println("<link rel='stylesheet' href='" + contextPath + "/css/style.css'>");
        out.println("</head><body class='page-shell'><div class='page-card'>");

        out.println("<header class='page-header'>");
        out.println("<div><h1>Content Detail</h1><p>Review the title details and start watching now.</p></div>");
        out.println("<nav class='nav-links'>");
        out.println("<a href='" + contextPath + "/home'>Browse</a>");
        out.println("<a href='" + contextPath + "/dashboard'>Dashboard</a>");
        if ("admin".equals(role)) out.println("<a href='" + contextPath + "/admin'>Admin</a>");
        out.println("<a href='" + contextPath + "/logout'>Logout</a>");
        out.println("</nav></header>");

        if (idParam == null) {
            out.println("<div class='detail-card'><p>No content selected.</p></div>");
            out.println("<div style='margin-top:24px'><a class='btn-secondary' href='" + contextPath + "/home'>Back to Browse</a></div>");
            out.println("</div></body></html>");
            return;
        }

        int id = Integer.parseInt(idParam);

        try (Connection c = DatabaseConnection.getConnection()) {

            // ── Main content row ──────────────────────────────────────────────
            String sql = "SELECT ci.content_id, ci.title, ci.type, ci.release_year, " +
                         "ci.age_rating, ci.average_rating, ci.total_reviews, " +
                         "ci.synopsis, s.studio_name " +
                         "FROM Content_Items ci " +
                         "LEFT JOIN Studios s ON ci.studio_id = s.studio_id " +
                         "WHERE ci.content_id = ?";

            try (PreparedStatement ps = c.prepareStatement(sql)) {
                ps.setInt(1, id);
                ResultSet rs = ps.executeQuery();
                if (rs.next()) {
                    String type     = rs.getString("type");
                    String synopsis = rs.getString("synopsis");
                    out.println("<div class='detail-card'>");
                    out.println("<h1>" + escapeHtml(rs.getString("title")) + "</h1>");
                    out.println("<p class='meta'>" + escapeHtml(type) + " &bull; " +
                                rs.getInt("release_year") + " &bull; " +
                                escapeHtml(rs.getString("age_rating")) + "</p>");
                    out.println("<p>Rating: <strong>" + rs.getBigDecimal("average_rating") + " / 10</strong> &bull; " +
                                rs.getInt("total_reviews") + " reviews</p>");
                    if (rs.getString("studio_name") != null) {
                        out.println("<p>Studio: " + escapeHtml(rs.getString("studio_name")) + "</p>");
                    }
                    if (synopsis != null && !synopsis.isEmpty()) {
                        out.println("<p style='margin-top:14px;line-height:1.7;color:rgba(215,205,255,0.82)'>" +
                                    escapeHtml(synopsis) + "</p>");
                    }

                    // ── Trailer placeholder ───────────────────────────────────
                    out.println("<div style='margin-top:22px;background:rgba(0,0,0,0.45);border:1px solid rgba(255,255,255,0.1);border-radius:12px;overflow:hidden;aspect-ratio:16/9;max-width:640px;display:flex;flex-direction:column;align-items:center;justify-content:center;gap:10px'>");
                    out.println("<div style='font-size:3rem;opacity:0.5'>&#127916;</div>");
                    out.println("<p style='color:rgba(200,190,255,0.6);font-size:0.9rem;margin:0'>Trailer not available</p>");
                    out.println("</div>");

                    out.println("<form method='post' action='" + contextPath + "/content?id=" + id + "' style='margin-top:20px'>");
                    out.println("<button class='btn-primary' type='submit'>&#9654; Play Now</button>");
                    out.println("</form>");
                    out.println("</div>");

                    // ── Episodes list (Series only) ───────────────────────────
                    if ("Series".equals(type)) {
                        out.println("<div class='section-title' style='margin-top:32px'>Episodes</div>");
                        out.println("<table class='content-table'><thead><tr>");
                        out.println("<th>Season</th><th>Episode</th><th>Title</th><th>Duration</th>");
                        out.println("</tr></thead><tbody>");

                        String epSql = "SELECT season_no, episode_no, title, duration_minutes " +
                                       "FROM Episodes WHERE content_id = ? ORDER BY season_no, episode_no";
                        try (PreparedStatement epPs = c.prepareStatement(epSql)) {
                            epPs.setInt(1, id);
                            ResultSet epRs = epPs.executeQuery();
                            int epCount = 0;
                            while (epRs.next()) {
                                epCount++;
                                out.println("<tr>");
                                out.println("<td>S" + String.format("%02d", epRs.getInt("season_no")) + "</td>");
                                out.println("<td>E" + String.format("%02d", epRs.getInt("episode_no")) + "</td>");
                                out.println("<td>" + escapeHtml(epRs.getString("title")) + "</td>");
                                out.println("<td>" + epRs.getInt("duration_minutes") + " min</td>");
                                out.println("</tr>");
                            }
                            if (epCount == 0) {
                                out.println("<tr><td colspan='4'>No episodes listed.</td></tr>");
                            }
                        }
                        out.println("</tbody></table>");
                    }

                    // ── User Reviews ──────────────────────────────────────────
                    out.println("<div class='section-title' style='margin-top:32px'>User Reviews</div>");
                    String revSql = "SELECT u.full_name, r.rating, r.review_text, r.review_date " +
                                    "FROM Reviews r JOIN Users u ON r.user_id = u.user_id " +
                                    "WHERE r.content_id = ? ORDER BY r.review_date DESC";
                    try (PreparedStatement revPs = c.prepareStatement(revSql)) {
                        revPs.setInt(1, id);
                        ResultSet revRs = revPs.executeQuery();
                        int revCount = 0;
                        while (revRs.next()) {
                            revCount++;
                            out.println("<div style='background:rgba(255,255,255,0.04);border:1px solid rgba(255,255,255,0.09);border-radius:10px;padding:16px 20px;margin-bottom:12px'>");
                            out.println("<div style='display:flex;justify-content:space-between;align-items:center;margin-bottom:8px'>");
                            out.println("<strong style='color:#d7ccff'>" + escapeHtml(revRs.getString("full_name")) + "</strong>");
                            out.println("<span style='color:#f0c040;font-size:0.9rem'>&#11088; " + revRs.getBigDecimal("rating") + " / 10</span>");
                            out.println("</div>");
                            out.println("<p style='color:rgba(210,200,255,0.8);margin:0 0 6px;line-height:1.6'>" + escapeHtml(revRs.getString("review_text")) + "</p>");
                            out.println("<small style='color:rgba(180,170,220,0.5)'>" + revRs.getString("review_date") + "</small>");
                            out.println("</div>");
                        }
                        if (revCount == 0) {
                            out.println("<p style='color:rgba(200,190,255,0.5)'>No reviews yet.</p>");
                        }
                    }

                } else {
                    out.println("<div class='detail-card'><p>Content not found.</p></div>");
                }
            }
        } catch (Exception e) {
            out.println("<div class='detail-card'><p>Error: " + escapeHtml(e.getMessage()) + "</p></div>");
        }

        out.println("<div style='margin-top:24px'><a class='btn-secondary' href='" + contextPath + "/home'>Back to Browse</a></div>");
        out.println("</div></body></html>");
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse res)
            throws ServletException, IOException {

        HttpSession session = req.getSession(false);
        if (session == null || session.getAttribute("user") == null) {
            res.sendRedirect(req.getContextPath() + "/login.html");
            return;
        }

        String idParam = req.getParameter("id");
        String userEmail = (String) session.getAttribute("user");

        if (idParam != null) {
            int contentId = Integer.parseInt(idParam);
            String sql = "INSERT INTO Watch_History (user_id, content_id, watch_date, progress_pct, completed) " +
                         "SELECT u.user_id, ?, CURDATE(), 0, FALSE " +
                         "FROM Users u WHERE u.email = ? LIMIT 1";
            try (Connection c = DatabaseConnection.getConnection();
                 PreparedStatement ps = c.prepareStatement(sql)) {
                ps.setInt(1, contentId);
                ps.setString(2, userEmail);
                ps.executeUpdate();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        res.sendRedirect(req.getContextPath() + "/content?id=" + idParam);
    }

    private static String escapeHtml(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
    }
}
