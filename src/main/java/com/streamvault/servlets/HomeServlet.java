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
import java.util.ArrayList;
import java.util.List;

@WebServlet("/home")
public class HomeServlet extends HttpServlet {

    private static final int PAGE_SIZE = 8;

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse res)
            throws ServletException, IOException {

        HttpSession session = req.getSession(false);
        if (session == null || session.getAttribute("user") == null) {
            res.sendRedirect(req.getContextPath() + "/login.html");
            return;
        }

        String email     = (String) session.getAttribute("user");
        String role      = (String) session.getAttribute("role");
        String type      = req.getParameter("type");
        String genre     = req.getParameter("genre");
        String ageRating = req.getParameter("age_rating");
        String language  = req.getParameter("language");
        String search    = req.getParameter("search");
        int page = 1;
        try { page = Math.max(1, Integer.parseInt(req.getParameter("page"))); } catch (Exception ignored) {}

        String contextPath = req.getContextPath();
        res.setContentType("text/html");
        PrintWriter out = res.getWriter();

        out.println("<!DOCTYPE html><html lang='en'><head><meta charset='UTF-8'>");
        out.println("<meta name='viewport' content='width=device-width, initial-scale=1.0'>");
        out.println("<title>StreamVault — Browse</title>");
        out.println("<link rel='stylesheet' href='" + contextPath + "/css/style.css'>");
        out.println("</head><body class='page-shell'><div class='page-card'>");

        out.println("<header class='page-header'>");
        out.println("<div><h1>StreamVault</h1><p>Browse the latest content and pick a title to watch.</p></div>");
        out.println("<nav class='nav-links'>");
        out.println("<a href='" + contextPath + "/home'>Browse</a>");
        out.println("<a href='" + contextPath + "/dashboard'>Dashboard</a>");
        if ("admin".equals(role)) out.println("<a href='" + contextPath + "/admin'>Admin</a>");
        out.println("<a href='" + contextPath + "/logout'>Logout</a>");
        out.println("</nav></header>");

        out.println("<div class='section-title'>Signed in as " + escapeHtml(email) + "</div>");
        out.println("<section class='content-panel'>");

        // ── Filter form ───────────────────────────────────────────────────────
        out.println("<form class='content-filter' method='get' action='" + contextPath + "/home'>");

        // Search bar
        out.println("<input type='text' name='search' placeholder='Search titles...' value='" +
                    escapeAttr(search) + "' class='filter-input'>");

        // Type dropdown
        out.println("<select name='type' class='filter-select'>");
        out.println("<option value=''>All Types</option>");
        for (String t : new String[]{"Movie", "Series", "Music"}) {
            out.println("<option value='" + t + "'" + (t.equals(type) ? " selected" : "") + ">" + t + "</option>");
        }
        out.println("</select>");

        // Genre dropdown — populated from DB
        out.println("<select name='genre' class='filter-select'>");
        out.println("<option value=''>All Genres</option>");
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement("SELECT genre_name FROM Genres ORDER BY genre_name");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                String g = rs.getString("genre_name");
                out.println("<option value='" + escapeAttr(g) + "'" +
                            (g.equals(genre) ? " selected" : "") + ">" + escapeHtml(g) + "</option>");
            }
        } catch (Exception ignored) {}
        out.println("</select>");

        // Age rating dropdown
        out.println("<select name='age_rating' class='filter-select'>");
        out.println("<option value=''>All Ratings</option>");
        for (String r : new String[]{"G", "PG", "PG-13", "R", "TV-14", "TV-MA"}) {
            out.println("<option value='" + r + "'" + (r.equals(ageRating) ? " selected" : "") + ">" + r + "</option>");
        }
        out.println("</select>");

        // Language dropdown — populated from DB
        out.println("<select name='language' class='filter-select'>");
        out.println("<option value=''>All Languages</option>");
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement("SELECT language_name FROM Languages ORDER BY language_name");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                String l = rs.getString("language_name");
                out.println("<option value='" + escapeAttr(l) + "'" +
                            (l.equals(language) ? " selected" : "") + ">" + escapeHtml(l) + "</option>");
            }
        } catch (Exception ignored) {}
        out.println("</select>");

        out.println("<button class='btn-primary' type='submit'>Apply Filter</button>");
        out.println("</form>");

        // ── Movie card grid ───────────────────────────────────────────────────
        out.println("<div class='movie-grid'>");

        // Dynamic query with genre JOIN when needed
        StringBuilder sql = new StringBuilder(
            "SELECT DISTINCT ci.content_id, ci.title, ci.type, ci.release_year, " +
            "ci.age_rating, ci.average_rating FROM Content_Items ci "
        );
        List<Object> params   = new ArrayList<>();
        List<String> clauses  = new ArrayList<>();

        if (genre != null && !genre.isEmpty()) {
            sql.append("JOIN Content_Genres cg ON ci.content_id = cg.content_id ")
               .append("JOIN Genres g ON cg.genre_id = g.genre_id ");
            clauses.add("g.genre_name = ?");
            params.add(genre);
        }
        if (language != null && !language.isEmpty()) {
            sql.append("JOIN Content_Languages cl ON ci.content_id = cl.content_id ")
               .append("JOIN Languages l ON cl.language_id = l.language_id ");
            clauses.add("l.language_name = ?");
            params.add(language);
        }
        if (type != null && !type.isEmpty()) {
            clauses.add("ci.type = ?");
            params.add(type);
        }
        if (ageRating != null && !ageRating.isEmpty()) {
            clauses.add("ci.age_rating = ?");
            params.add(ageRating);
        }
        if (search != null && !search.isEmpty()) {
            clauses.add("ci.title LIKE ?");
            params.add("%" + search + "%");
        }
        if (!clauses.isEmpty()) {
            sql.append("WHERE ").append(String.join(" AND ", clauses)).append(" ");
        }
        sql.append("ORDER BY ci.average_rating DESC LIMIT ? OFFSET ?");
        params.add(PAGE_SIZE);
        params.add((page - 1) * PAGE_SIZE);

        int rowCount = 0;
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++) {
                Object p = params.get(i);
                if (p instanceof Integer) ps.setInt(i + 1, (Integer) p);
                else ps.setString(i + 1, (String) p);
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    rowCount++;
                    String t   = rs.getString("type");
                    String bg  = posterGradient(t);
                    out.println("<a class='movie-card' href='" + contextPath + "/content?id=" + rs.getInt("content_id") + "'>");
                    out.println("<div class='movie-card-poster' style='background:" + bg + "'>");
                    out.println("<span class='movie-card-type-label'>" + escapeHtml(t) + "</span>");
                    out.println("</div>");
                    out.println("<div class='movie-card-body'>");
                    out.println("<div class='movie-card-title'>" + escapeHtml(rs.getString("title")) + "</div>");
                    out.println("<div class='movie-card-meta'>" + rs.getInt("release_year") +
                                "<span class='movie-card-age'>" + escapeHtml(rs.getString("age_rating")) + "</span></div>");
                    out.println("<div class='movie-card-rating'>&#11088; " + rs.getBigDecimal("average_rating") + "</div>");
                    out.println("</div></a>");
                }
            }
        } catch (Exception e) {
            out.println("<p style='color:#f7c6ff;padding:16px'>Error: " + escapeHtml(e.getMessage()) + "</p>");
        }

        if (rowCount == 0) {
            out.println("<p style='color:rgba(210,200,255,0.6);padding:20px 0'>No content matches your filters.</p>");
        }
        out.println("</div>");

        // ── Pagination ────────────────────────────────────────────────────────
        out.println("<div style='display:flex;gap:12px;margin-top:20px;align-items:center'>");
        if (page > 1) {
            out.println("<a class='btn-secondary' href='" +
                        pageUrl(contextPath, type, genre, ageRating, language, search, page - 1) + "'>&larr; Prev</a>");
        }
        out.println("<span style='color:rgba(200,190,255,0.7);font-size:0.9rem'>Page " + page + "</span>");
        if (rowCount == PAGE_SIZE) {
            out.println("<a class='btn-secondary' href='" +
                        pageUrl(contextPath, type, genre, ageRating, language, search, page + 1) + "'>Next &rarr;</a>");
        }
        out.println("</div>");

        out.println("</section></div></body></html>");
    }

    private String pageUrl(String ctx, String type, String genre, String age, String language, String search, int page) {
        StringBuilder url = new StringBuilder(ctx + "/home?page=" + page);
        if (type     != null && !type.isEmpty())     url.append("&type=").append(escapeAttr(type));
        if (genre    != null && !genre.isEmpty())    url.append("&genre=").append(escapeAttr(genre));
        if (age      != null && !age.isEmpty())      url.append("&age_rating=").append(escapeAttr(age));
        if (language != null && !language.isEmpty()) url.append("&language=").append(escapeAttr(language));
        if (search   != null && !search.isEmpty())   url.append("&search=").append(escapeAttr(search));
        return url.toString();
    }

    private static String posterGradient(String type) {
        if ("Movie".equals(type))  return "linear-gradient(135deg,#1a0e4a 0%,#4a1d8a 100%)";
        if ("Series".equals(type)) return "linear-gradient(135deg,#0a2040 0%,#1a4a8a 100%)";
        if ("Music".equals(type))  return "linear-gradient(135deg,#2a0840 0%,#6b0a8a 100%)";
        return "linear-gradient(135deg,#1a1040 0%,#3a2060 100%)";
    }

    private static String escapeHtml(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
    }

    private static String escapeAttr(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("\"", "&quot;").replace("'", "&#39;");
    }
}
