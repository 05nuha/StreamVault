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

@WebServlet("/dashboard")
public class DashboardServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse res)
            throws ServletException, IOException {

        HttpSession session = req.getSession(false);
        if (session == null || session.getAttribute("user") == null) {
            res.sendRedirect(req.getContextPath() + "/login.html");
            return;
        }

        String email       = (String) session.getAttribute("user");
        String role        = (String) session.getAttribute("role");
        String contextPath = req.getContextPath();
        res.setContentType("text/html");
        PrintWriter out = res.getWriter();

        out.println("<!DOCTYPE html><html lang='en'><head><meta charset='UTF-8'>");
        out.println("<meta name='viewport' content='width=device-width, initial-scale=1.0'>");
        out.println("<title>My Dashboard — StreamVault</title>");
        out.println("<link rel='stylesheet' href='" + contextPath + "/css/style.css'>");
        out.println("</head><body class='page-shell'><div class='page-card'>");

        out.println("<header class='page-header'>");
        out.println("<div><h1>My Dashboard</h1><p>Manage your subscription and track what you watched recently.</p></div>");
        out.println("<nav class='nav-links'>");
        out.println("<a href='" + contextPath + "/home'>Browse</a>");
        out.println("<a href='" + contextPath + "/dashboard'>Dashboard</a>");
        if ("admin".equals(role)) out.println("<a href='" + contextPath + "/admin'>Admin</a>");
        out.println("<a href='" + contextPath + "/logout'>Logout</a>");
        out.println("</nav></header>");

        // ── Account + Subscription cards ──────────────────────────────────────
        out.println("<div class='card-grid'>");
        out.println("<div class='stat-card'><h3>Account</h3><p>Signed in as<br><strong>" + escapeHtml(email) + "</strong></p></div>");

        String subSql = "SELECT s.status, s.start_date, s.auto_renew, p.plan_name, p.monthly_price " +
                        "FROM Subscriptions s " +
                        "JOIN Subscription_Plans p ON s.plan_id = p.plan_id " +
                        "JOIN Users u ON s.user_id = u.user_id " +
                        "WHERE u.email = ? AND s.status = 'active'";

        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(subSql)) {
            ps.setString(1, email);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    out.println("<div class='stat-card'><h3>Subscription</h3>");
                    out.println("<p><strong>" + escapeHtml(rs.getString("plan_name")) + "</strong></p>");
                    out.println("<p>$" + String.format("%.2f", rs.getDouble("monthly_price")) + " / month</p>");
                    out.println("<p>Started: " + rs.getString("start_date") + "</p>");
                    out.println("<p>Auto-renew: " + (rs.getBoolean("auto_renew") ? "Yes" : "No") + "</p>");
                    out.println("<p><span class='badge-yes'>Active</span></p></div>");
                } else {
                    out.println("<div class='stat-card'><h3>Subscription</h3>");
                    out.println("<p>No active subscription found.</p></div>");
                }
            }
        } catch (Exception e) {
            out.println("<div class='stat-card'><h3>Subscription</h3><p>Error: " + escapeHtml(e.getMessage()) + "</p></div>");
        }
        out.println("</div>");

        // ── Billing History ───────────────────────────────────────────────────
        out.println("<div class='section-title' style='margin-top:32px'>Billing History</div>");
        out.println("<table class='content-table'><thead><tr>");
        out.println("<th>Plan</th><th>Amount</th><th>Date</th><th>Status</th>");
        out.println("</tr></thead><tbody>");

        // Note: status column added in updated setup.sql; falls back gracefully if absent
        String billSql = "SELECT pay.amount, pay.payment_date, p.plan_name, " +
                         "COALESCE(pay.status,'completed') AS pay_status " +
                         "FROM Payments pay " +
                         "JOIN Subscriptions s ON pay.subscription_id = s.subscription_id " +
                         "JOIN Subscription_Plans p ON s.plan_id = p.plan_id " +
                         "JOIN Users u ON s.user_id = u.user_id " +
                         "WHERE u.email = ? ORDER BY pay.payment_date DESC LIMIT 5";

        int billCount = 0;
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(billSql)) {
            ps.setString(1, email);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    billCount++;
                    String st = rs.getString("pay_status");
                    out.println("<tr>");
                    out.println("<td>" + escapeHtml(rs.getString("plan_name")) + "</td>");
                    out.println("<td>$" + String.format("%.2f", rs.getDouble("amount")) + "</td>");
                    out.println("<td>" + rs.getString("payment_date") + "</td>");
                    out.println("<td><span class='" + ("completed".equals(st) ? "badge-yes" : "badge-no") + "'>" + escapeHtml(st) + "</span></td>");
                    out.println("</tr>");
                }
            }
        } catch (Exception e) {
            out.println("<tr><td colspan='4'>Error: " + escapeHtml(e.getMessage()) + "</td></tr>");
        }
        if (billCount == 0) out.println("<tr><td colspan='4'>No billing records found.</td></tr>");
        out.println("</tbody></table>");

        // ── Continue Watching (progress_pct < 100) ────────────────────────────
        out.println("<div class='section-title' style='margin-top:32px'>Continue Watching</div>");
        out.println("<table class='content-table'><thead><tr>");
        out.println("<th>Title</th><th>Last Watched</th><th>Progress</th><th>Resume</th>");
        out.println("</tr></thead><tbody>");

        String cwSql = "SELECT ci.content_id, ci.title, wh.watch_date, wh.progress_pct " +
                       "FROM Watch_History wh " +
                       "JOIN Content_Items ci ON wh.content_id = ci.content_id " +
                       "JOIN Users u ON wh.user_id = u.user_id " +
                       "WHERE u.email = ? AND wh.completed = FALSE AND wh.progress_pct > 0 " +
                       "ORDER BY wh.watch_date DESC";

        int cwCount = 0;
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(cwSql)) {
            ps.setString(1, email);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    cwCount++;
                    int pct = rs.getInt("progress_pct");
                    out.println("<tr>");
                    out.println("<td>" + escapeHtml(rs.getString("title")) + "</td>");
                    out.println("<td>" + rs.getString("watch_date") + "</td>");
                    out.println("<td><div class='progress-wrap'><div class='progress-bar' style='width:" + pct + "%'></div><span>" + pct + "%</span></div></td>");
                    out.println("<td><a class='btn-secondary' style='padding:6px 14px;font-size:0.8rem' href='" + contextPath + "/content?id=" + rs.getInt("content_id") + "'>&#9654; Resume</a></td>");
                    out.println("</tr>");
                }
            }
        } catch (Exception e) {
            out.println("<tr><td colspan='4'>Error: " + escapeHtml(e.getMessage()) + "</td></tr>");
        }
        if (cwCount == 0) out.println("<tr><td colspan='4'>No in-progress titles.</td></tr>");
        out.println("</tbody></table>");

        // ── Watch History (last 30 days) ──────────────────────────────────────
        out.println("<div class='section-title' style='margin-top:32px'>Recent Watch History " +
                    "<span style='font-size:0.8rem;font-weight:400;color:rgba(210,200,255,0.55)'>(last 30 days)</span></div>");
        out.println("<table class='content-table'><thead><tr>");
        out.println("<th>Title</th><th>Date</th><th>Progress</th><th>Completed</th>");
        out.println("</tr></thead><tbody>");

        String historySql = "SELECT ci.title, wh.watch_date, wh.progress_pct, wh.completed " +
                            "FROM Watch_History wh " +
                            "JOIN Content_Items ci ON wh.content_id = ci.content_id " +
                            "JOIN Users u ON wh.user_id = u.user_id " +
                            "WHERE u.email = ? AND wh.watch_date >= DATE_SUB(CURDATE(), INTERVAL 30 DAY) " +
                            "ORDER BY wh.watch_date DESC";

        int histCount = 0;
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(historySql)) {
            ps.setString(1, email);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    histCount++;
                    int pct  = rs.getInt("progress_pct");
                    boolean done = rs.getBoolean("completed");
                    out.println("<tr>");
                    out.println("<td>" + escapeHtml(rs.getString("title")) + "</td>");
                    out.println("<td>" + rs.getString("watch_date") + "</td>");
                    out.println("<td><div class='progress-wrap'><div class='progress-bar' style='width:" + pct + "%'></div><span>" + pct + "%</span></div></td>");
                    out.println("<td><span class='" + (done ? "badge-yes" : "badge-no") + "'>" + (done ? "Yes" : "No") + "</span></td>");
                    out.println("</tr>");
                }
            }
        } catch (Exception e) {
            out.println("<tr><td colspan='4'>Error: " + escapeHtml(e.getMessage()) + "</td></tr>");
        }
        if (histCount == 0) out.println("<tr><td colspan='4'>No watch history in the last 30 days.</td></tr>");
        out.println("</tbody></table>");

        out.println("<div style='margin-top:28px;display:flex;gap:16px;flex-wrap:wrap;'>");
        out.println("<a class='btn-primary' href='" + contextPath + "/home'>Browse Content</a>");
        out.println("<a class='btn-secondary' href='" + contextPath + "/logout'>Logout</a>");
        out.println("</div>");
        out.println("</div></body></html>");
    }

    private static String escapeHtml(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
    }
}
