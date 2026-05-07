package com.streamvault.servlets;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.streamvault.db.DatabaseConnection;
import com.streamvault.db.MongoConnection;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.bson.Document;

import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

@WebServlet("/admin")
public class AdminServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse res)
            throws ServletException, IOException {

        HttpSession session = req.getSession(false);
        if (session == null || session.getAttribute("user") == null) {
            res.sendRedirect(req.getContextPath() + "/login.html");
            return;
        }

        // ── Role guard ────────────────────────────────────────────────────────
        String role = (String) session.getAttribute("role");
        if (!"admin".equals(role)) {
            res.sendRedirect(req.getContextPath() + "/home");
            return;
        }

        String contextPath = req.getContextPath();
        res.setContentType("text/html");
        PrintWriter out = res.getWriter();

        out.println("<!DOCTYPE html><html lang='en'><head><meta charset='UTF-8'>");
        out.println("<meta name='viewport' content='width=device-width, initial-scale=1.0'>");
        out.println("<title>Admin Analytics — StreamVault</title>");
        out.println("<link rel='stylesheet' href='" + contextPath + "/css/style.css'>");
        out.println("</head><body class='page-shell'><div class='page-card'>");

        out.println("<header class='page-header'>");
        out.println("<div>");
        out.println("<h1>Admin Analytics <span class='admin-badge'>Admin</span></h1>");
        out.println("<p>Live insights from MySQL and MongoDB — subscriber stats, revenue, and watch behaviour.</p>");
        out.println("</div>");
        out.println("<nav class='nav-links'>");
        out.println("<a href='" + contextPath + "/home'>Browse</a>");
        out.println("<a href='" + contextPath + "/dashboard'>Dashboard</a>");
        out.println("<a href='" + contextPath + "/admin' class='nav-active'>Admin</a>");
        out.println("<a href='" + contextPath + "/logout'>Logout</a>");
        out.println("</nav></header>");

        // ── 4 Stat cards (MySQL) ──────────────────────────────────────────────
        out.println("<div class='card-grid' style='grid-template-columns:repeat(auto-fit,minmax(200px,1fr))'>");

        statCard(out, "New Subscribers", "#c084fc", newSubsSql(), "This month");
        statCard(out, "Active Subscribers", "#818cf8", "SELECT COUNT(*) AS v FROM Subscriptions WHERE status='active'", "All time");
        statCard(out, "Total Watch Events", "#a78bfa", "SELECT COUNT(*) AS v FROM Watch_History", "All time");
        revenueStatCard(out);

        out.println("</div>");

        // ── MongoDB ───────────────────────────────────────────────────────────
        try {
            MongoDatabase db          = MongoConnection.getDatabase();
            MongoCollection<Document> watchHistory = db.getCollection("watch_history");
            MongoCollection<Document> users        = db.getCollection("users");

            // Pipeline 1: Top-10 most-completed titles
            sectionHeader(out, "Top 10 Most-Completed Titles", "MongoDB");
            out.println("<table class='content-table'><thead><tr>");
            out.println("<th>#</th><th>Title</th><th>Completions</th><th>Avg Progress</th>");
            out.println("</tr></thead><tbody>");

            List<Document> top10Pipeline = Arrays.asList(
                new Document("$match", new Document("completed", true)),
                new Document("$group", new Document("_id", "$content_title")
                    .append("total_completions", new Document("$sum", 1))
                    .append("avg_progress",      new Document("$avg", "$progress_pct"))),
                new Document("$sort",  new Document("total_completions", -1)),
                new Document("$limit", 10)
            );

            int rank = 1; boolean anyRow = false;
            for (Document doc : watchHistory.aggregate(top10Pipeline)) {
                anyRow = true;
                Double avgPct = doc.getDouble("avg_progress");
                out.println("<tr>");
                out.println("<td><strong>#" + rank++ + "</strong></td>");
                out.println("<td>" + escapeHtml(doc.getString("_id")) + "</td>");
                out.println("<td>" + doc.getInteger("total_completions", 0) + "</td>");
                out.println("<td>" + (avgPct != null ? String.format("%.1f", avgPct) : "N/A") + "%</td>");
                out.println("</tr>");
            }
            if (!anyRow) out.println("<tr><td colspan='4' style='color:rgba(200,185,255,0.5)'>No completed watches recorded.</td></tr>");
            out.println("</tbody></table></section>");

            // Pipeline 2: Genre popularity by country (top 20)
            sectionHeader(out, "Genre Popularity by Country", "MongoDB");
            out.println("<table class='content-table'><thead><tr>");
            out.println("<th>Country</th><th>Genre</th><th>Watch Count</th>");
            out.println("</tr></thead><tbody>");

            List<Document> genrePipeline = Arrays.asList(
                new Document("$unwind", "$genre"),
                new Document("$group", new Document("_id",
                        new Document("country", "$country").append("genre", "$genre"))
                    .append("watch_count", new Document("$sum", 1))),
                new Document("$project", new Document("_id", 0)
                    .append("country",     "$_id.country")
                    .append("genre",       "$_id.genre")
                    .append("watch_count", 1)),
                new Document("$sort",  new Document("watch_count", -1)),
                new Document("$limit", 20)
            );

            anyRow = false;
            for (Document doc : watchHistory.aggregate(genrePipeline)) {
                anyRow = true;
                out.println("<tr>");
                out.println("<td>" + escapeHtml(doc.getString("country")) + "</td>");
                out.println("<td>" + escapeHtml(doc.getString("genre"))   + "</td>");
                out.println("<td>" + doc.getInteger("watch_count", 0)     + "</td>");
                out.println("</tr>");
            }
            if (!anyRow) out.println("<tr><td colspan='3' style='color:rgba(200,185,255,0.5)'>No genre data found.</td></tr>");
            out.println("</tbody></table></section>");

            // Pipeline 3: Churn-risk users (inactive 30+ days), sorted by days inactive
            sectionHeader(out, "Churn-Risk Users — Inactive 30+ Days", "MongoDB");
            out.println("<table class='content-table'><thead><tr>");
            out.println("<th>Name</th><th>Email</th><th>Plan</th><th>Days Inactive</th>");
            out.println("</tr></thead><tbody>");

            Date cutoff = new Date(System.currentTimeMillis() - 30L * 24 * 60 * 60 * 1000);
            List<Document> churnPipeline = Arrays.asList(
                new Document("$match", new Document("last_active", new Document("$lt", cutoff))),
                new Document("$group", new Document("_id", "$_id")
                    .append("full_name",   new Document("$first", "$full_name"))
                    .append("email",       new Document("$first", "$email"))
                    .append("last_active", new Document("$first", "$last_active"))
                    .append("plan",        new Document("$first", "$subscription.plan_name"))),
                new Document("$project", new Document("_id", 0)
                    .append("full_name",    1).append("email", 1)
                    .append("last_active",  1).append("plan",  1)
                    .append("days_inactive", new Document("$round", Arrays.asList(
                        new Document("$divide", Arrays.asList(
                            new Document("$subtract", Arrays.asList(new Date(), "$last_active")),
                            1000L * 60 * 60 * 24
                        )), 0)))),
                new Document("$sort",  new Document("days_inactive", -1)),
                new Document("$limit", 25)
            );

            anyRow = false;
            for (Document doc : users.aggregate(churnPipeline)) {
                anyRow = true;
                Object days = doc.get("days_inactive");
                out.println("<tr>");
                out.println("<td>" + escapeHtml(doc.getString("full_name")) + "</td>");
                out.println("<td>" + escapeHtml(doc.getString("email"))     + "</td>");
                out.println("<td>" + escapeHtml(doc.getString("plan"))      + "</td>");
                out.println("<td><span class='badge-no'>" + days + " days</span></td>");
                out.println("</tr>");
            }
            if (!anyRow) out.println("<tr><td colspan='4' style='color:rgba(200,185,255,0.5)'>No churn-risk users found.</td></tr>");
            out.println("</tbody></table></section>");

        } catch (Exception e) {
            out.println("<div class='msg-error' style='display:block;margin-top:20px'>MongoDB error: " + escapeHtml(e.getMessage()) + "</div>");
        }

        // ── MySQL: Content Rankings with RANK() window function ───────────────
        sectionHeader(out, "Content Rankings by Watch Count — RANK() Window Function", "MySQL");
        out.println("<table class='content-table'><thead><tr>");
        out.println("<th>Rank</th><th>Title</th><th>Type</th><th>Watch Count</th><th>Completions</th>");
        out.println("</tr></thead><tbody>");

        String rankSql =
            "SELECT ci.title, ci.type, " +
            "COUNT(wh.history_id) AS watch_count, " +
            "SUM(CASE WHEN wh.completed = TRUE THEN 1 ELSE 0 END) AS completions, " +
            "RANK() OVER (ORDER BY COUNT(wh.history_id) DESC) AS content_rank " +
            "FROM Content_Items ci " +
            "LEFT JOIN Watch_History wh ON ci.content_id = wh.content_id " +
            "GROUP BY ci.content_id, ci.title, ci.type " +
            "ORDER BY content_rank";

        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(rankSql);
             ResultSet rs = ps.executeQuery()) {
            boolean any = false;
            while (rs.next()) {
                any = true;
                int r = rs.getInt("content_rank");
                out.println("<tr>");
                out.println("<td><span style='font-weight:700;color:" + rankColor(r) + "'>#" + r + "</span></td>");
                out.println("<td>" + escapeHtml(rs.getString("title")) + "</td>");
                out.println("<td>" + escapeHtml(rs.getString("type"))  + "</td>");
                out.println("<td>" + rs.getInt("watch_count")          + "</td>");
                out.println("<td>" + rs.getInt("completions")          + "</td>");
                out.println("</tr>");
            }
            if (!any) out.println("<tr><td colspan='5' style='color:rgba(200,185,255,0.5)'>No data.</td></tr>");
        } catch (Exception e) {
            out.println("<tr><td colspan='5'>Error: " + escapeHtml(e.getMessage()) + "</td></tr>");
        }
        out.println("</tbody></table></section>");

        // ── MySQL: Revenue by Plan — This Month ───────────────────────────────
        sectionHeader(out, "Revenue by Plan — This Month", "MySQL");
        out.println("<table class='content-table'><thead><tr>");
        out.println("<th>Plan</th><th>Revenue</th><th>Payments</th>");
        out.println("</tr></thead><tbody>");

        String revMonthSql =
            "SELECT p.plan_name, SUM(pay.amount) AS revenue, COUNT(pay.payment_id) AS cnt " +
            "FROM Payments pay " +
            "JOIN Subscriptions s   ON pay.subscription_id = s.subscription_id " +
            "JOIN Subscription_Plans p ON s.plan_id = p.plan_id " +
            "WHERE MONTH(pay.payment_date) = MONTH(CURDATE()) AND YEAR(pay.payment_date) = YEAR(CURDATE()) " +
            "GROUP BY p.plan_name ORDER BY revenue DESC";

        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(revMonthSql);
             ResultSet rs = ps.executeQuery()) {
            boolean any = false;
            while (rs.next()) {
                any = true;
                out.println("<tr>");
                out.println("<td><strong>" + escapeHtml(rs.getString("plan_name")) + "</strong></td>");
                out.println("<td style='color:#a8ffd0;font-weight:600'>$" + String.format("%.2f", rs.getDouble("revenue")) + "</td>");
                out.println("<td>" + rs.getInt("cnt") + "</td>");
                out.println("</tr>");
            }
            if (!any) out.println("<tr><td colspan='3' style='color:rgba(200,185,255,0.5)'>No payments recorded this month.</td></tr>");
        } catch (Exception e) {
            out.println("<tr><td colspan='3'>Error: " + escapeHtml(e.getMessage()) + "</td></tr>");
        }
        out.println("</tbody></table></section>");

        // ── MySQL: Revenue by Plan — All Time ─────────────────────────────────
        sectionHeader(out, "Revenue by Plan — All Time", "MySQL");
        out.println("<table class='content-table'><thead><tr>");
        out.println("<th>Plan</th><th>Total Revenue</th><th>Payment Count</th>");
        out.println("</tr></thead><tbody>");

        String revAllSql =
            "SELECT p.plan_name, SUM(pay.amount) AS revenue, COUNT(pay.payment_id) AS cnt " +
            "FROM Payments pay " +
            "JOIN Subscriptions s   ON pay.subscription_id = s.subscription_id " +
            "JOIN Subscription_Plans p ON s.plan_id = p.plan_id " +
            "GROUP BY p.plan_name ORDER BY revenue DESC";

        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(revAllSql);
             ResultSet rs = ps.executeQuery()) {
            boolean any = false;
            while (rs.next()) {
                any = true;
                out.println("<tr>");
                out.println("<td><strong>" + escapeHtml(rs.getString("plan_name")) + "</strong></td>");
                out.println("<td style='color:#a8ffd0;font-weight:600'>$" + String.format("%.2f", rs.getDouble("revenue")) + "</td>");
                out.println("<td>" + rs.getInt("cnt") + "</td>");
                out.println("</tr>");
            }
            if (!any) out.println("<tr><td colspan='3' style='color:rgba(200,185,255,0.5)'>No payment data.</td></tr>");
        } catch (Exception e) {
            out.println("<tr><td colspan='3'>Error: " + escapeHtml(e.getMessage()) + "</td></tr>");
        }
        out.println("</tbody></table></section>");

        out.println("</div></body></html>");
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private static String newSubsSql() {
        return "SELECT COUNT(*) AS v FROM Subscriptions " +
               "WHERE MONTH(start_date)=MONTH(CURDATE()) AND YEAR(start_date)=YEAR(CURDATE())";
    }

    private void statCard(PrintWriter out, String label, String color, String sql, String sub) {
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                out.println("<div class='stat-card'>");
                out.println("<h3>" + label + "</h3>");
                out.println("<p style='font-size:2.2rem;font-weight:800;color:" + color + ";line-height:1'>" + rs.getInt("v") + "</p>");
                out.println("<p style='margin-top:8px'>" + sub + "</p>");
                out.println("</div>");
            }
        } catch (Exception e) {
            out.println("<div class='stat-card'><h3>" + label + "</h3><p>Error</p></div>");
        }
    }

    private void revenueStatCard(PrintWriter out) {
        String sql = "SELECT SUM(pay.amount) AS v FROM Payments pay WHERE pay.status='completed'";
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                double v = rs.getDouble("v");
                out.println("<div class='stat-card'>");
                out.println("<h3>Total Revenue</h3>");
                out.println("<p style='font-size:2.2rem;font-weight:800;color:#34d399;line-height:1'>$" + String.format("%.0f", v) + "</p>");
                out.println("<p style='margin-top:8px'>All time</p>");
                out.println("</div>");
            }
        } catch (Exception e) {
            out.println("<div class='stat-card'><h3>Total Revenue</h3><p>Error</p></div>");
        }
    }

    private void sectionHeader(PrintWriter out, String title, String source) {
        String badgeClass = "MongoDB".equals(source) ? "badge-mongo" : "badge-mysql";
        out.println("<section class='content-panel admin-section'>");
        out.println("<div class='admin-section-title'>");
        out.println("<span>" + title + "</span>");
        out.println("<span class='" + badgeClass + "'>" + source + "</span>");
        out.println("</div>");
    }

    private static String rankColor(int rank) {
        if (rank == 1) return "#fbbf24";
        if (rank == 2) return "#94a3b8";
        if (rank == 3) return "#c084fc";
        return "#e2d9ff";
    }

    private static String escapeHtml(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
    }
}
