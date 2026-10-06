package com.foundit;

import java.io.IOException;
import java.io.PrintWriter;
import java.sql.*;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;

@WebServlet("/items")
public class ItemServlet extends HttpServlet {

    private Integer uid(HttpServletRequest req) {
        HttpSession s = req.getSession(false);
        return (s == null) ? null : (Integer) s.getAttribute("userId");
    }

    protected void doGet(HttpServletRequest req, HttpServletResponse res)
            throws ServletException, IOException {
        res.setContentType("application/json");
        PrintWriter out = res.getWriter();
        if ("one".equals(req.getParameter("action"))) getOne(req, out);
        else list(req, out);
    }

    private void list(HttpServletRequest req, PrintWriter out) {
        String type = req.getParameter("type");
        String q    = req.getParameter("q");

        StringBuilder sql = new StringBuilder(
            "SELECT i.item_id,i.item_type,i.title,i.description,i.location,i.status,"
          + "i.created_at,u.name AS poster FROM items i "
          + "JOIN users u ON u.user_id=i.user_id WHERE i.status<>'ARCHIVED'");

        boolean hasType = type != null && !type.trim().isEmpty();
        boolean hasQ    = q != null && !q.trim().isEmpty();

        if (hasType) sql.append(" AND i.item_type=?");
        if (hasQ)    sql.append(" AND (i.title LIKE ? OR i.description LIKE ?)");
        sql.append(" ORDER BY i.item_id DESC");

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql.toString())) {

            int p = 1;
            if (hasType) ps.setString(p++, type);
            if (hasQ) { ps.setString(p++, "%" + q + "%"); ps.setString(p++, "%" + q + "%"); }

            ResultSet rs = ps.executeQuery();
            StringBuilder sb = new StringBuilder("{\"success\":true,\"items\":[");
            boolean first = true;
            while (rs.next()) {
                if (!first) sb.append(",");
                first = false;
                sb.append("{\"itemId\":").append(rs.getInt("item_id"))
                  .append(",\"itemType\":\"").append(DBConnection.esc(rs.getString("item_type")))
                  .append("\",\"title\":\"").append(DBConnection.esc(rs.getString("title")))
                  .append("\",\"description\":\"").append(DBConnection.esc(rs.getString("description")))
                  .append("\",\"location\":\"").append(DBConnection.esc(rs.getString("location")))
                  .append("\",\"status\":\"").append(DBConnection.esc(rs.getString("status")))
                  .append("\",\"poster\":\"").append(DBConnection.esc(rs.getString("poster")))
                  .append("\",\"createdAt\":\"").append(rs.getTimestamp("created_at"))
                  .append("\"}");
            }
            sb.append("]}");
            out.print(sb);

        } catch (SQLException e) {
            e.printStackTrace();
            out.print("{\"success\":false,\"message\":\"Server error\"}");
        }
    }

    private void getOne(HttpServletRequest req, PrintWriter out) {
        Integer me = uid(req);
        int id;
        try { id = Integer.parseInt(req.getParameter("id")); }
        catch (Exception e) { out.print("{\"success\":false,\"message\":\"Bad id\"}"); return; }

        String sql = "SELECT i.*, u.name AS poster FROM items i "
                   + "JOIN users u ON u.user_id=i.user_id WHERE i.item_id=?";

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, id);
            ResultSet rs = ps.executeQuery();
            if (!rs.next()) { out.print("{\"success\":false,\"message\":\"Not found\"}"); return; }

            boolean isOwner = (me != null && me == rs.getInt("user_id"));

            StringBuilder sb = new StringBuilder("{\"success\":true,\"item\":{");
            sb.append("\"itemId\":").append(rs.getInt("item_id"))
              .append(",\"itemType\":\"").append(DBConnection.esc(rs.getString("item_type")))
              .append("\",\"title\":\"").append(DBConnection.esc(rs.getString("title")))
              .append("\",\"description\":\"").append(DBConnection.esc(rs.getString("description")))
              .append("\",\"location\":\"").append(DBConnection.esc(rs.getString("location")))
              .append("\",\"status\":\"").append(DBConnection.esc(rs.getString("status")))
              .append("\",\"poster\":\"").append(DBConnection.esc(rs.getString("poster")))
              .append("\",\"isOwner\":").append(isOwner)
              .append(",\"secretQuestion\":\"").append(DBConnection.esc(rs.getString("secret_question")))
              .append("\",\"createdAt\":\"").append(rs.getTimestamp("created_at")).append("\"}}");
            out.print(sb);

        } catch (SQLException e) {
            e.printStackTrace();
            out.print("{\"success\":false,\"message\":\"Server error\"}");
        }
    }

    protected void doPost(HttpServletRequest req, HttpServletResponse res)
            throws ServletException, IOException {
        res.setContentType("application/json");
        PrintWriter out = res.getWriter();

        Integer me = uid(req);
        if (me == null) { out.print("{\"success\":false,\"message\":\"Please log in\"}"); return; }

        String type  = req.getParameter("itemType");
        String title = req.getParameter("title");
        String desc  = req.getParameter("description");
        String loc   = req.getParameter("location");
        String sq    = req.getParameter("secretQuestion");

        if (title == null || title.trim().isEmpty() || sq == null || sq.trim().isEmpty()
                || !("LOST".equals(type) || "FOUND".equals(type))) {
            out.print("{\"success\":false,\"message\":\"Title, type and verification question are required\"}");
            return;
        }

        String sql = "INSERT INTO items (user_id,item_type,title,description,location,secret_question) "
                   + "VALUES (?,?,?,?,?,?)";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, me);
            ps.setString(2, type);
            ps.setString(3, title.trim());
            ps.setString(4, desc == null ? "" : desc.trim());
            ps.setString(5, loc == null ? "" : loc.trim());
            ps.setString(6, sq.trim());
            ps.executeUpdate();
            out.print("{\"success\":true,\"message\":\"Item posted\"}");
        } catch (SQLException e) {
            e.printStackTrace();
            out.print("{\"success\":false,\"message\":\"Server error\"}");
        }
    }
}