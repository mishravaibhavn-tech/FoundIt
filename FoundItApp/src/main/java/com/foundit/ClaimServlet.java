package com.foundit;

import java.io.IOException;
import java.io.PrintWriter;
import java.sql.*;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;

@WebServlet("/claims")
public class ClaimServlet extends HttpServlet {

    private Integer uid(HttpServletRequest req) {
        HttpSession s = req.getSession(false);
        return (s == null) ? null : (Integer) s.getAttribute("userId");
    }

    protected void doGet(HttpServletRequest req, HttpServletResponse res)
            throws ServletException, IOException {
        res.setContentType("application/json");
        PrintWriter out = res.getWriter();

        Integer me = uid(req);
        if (me == null) { out.print("{\"success\":false,\"message\":\"Please log in\"}"); return; }

        boolean incoming = "incoming".equals(req.getParameter("action"));

        String sql = incoming
            ? "SELECT c.*, i.title, u.name AS claimant FROM claims c "
            + "JOIN items i ON i.item_id=c.item_id JOIN users u ON u.user_id=c.claimant_id "
            + "WHERE i.user_id=? ORDER BY c.claim_id DESC"
            : "SELECT c.*, i.title, u.name AS claimant FROM claims c "
            + "JOIN items i ON i.item_id=c.item_id JOIN users u ON u.user_id=c.claimant_id "
            + "WHERE c.claimant_id=? ORDER BY c.claim_id DESC";

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, me);
            ResultSet rs = ps.executeQuery();

            StringBuilder sb = new StringBuilder("{\"success\":true,\"claims\":[");
            boolean first = true;
            while (rs.next()) {
                if (!first) sb.append(",");
                first = false;
                sb.append("{\"claimId\":").append(rs.getInt("claim_id"))
                  .append(",\"itemId\":").append(rs.getInt("item_id"))
                  .append(",\"title\":\"").append(DBConnection.esc(rs.getString("title")))
                  .append("\",\"claimant\":\"").append(DBConnection.esc(rs.getString("claimant")))
                  .append("\",\"answer\":\"").append(DBConnection.esc(rs.getString("answer")))
                  .append("\",\"status\":\"").append(DBConnection.esc(rs.getString("status")))
                  .append("\"}");
            }
            sb.append("]}");
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

        if ("decide".equals(req.getParameter("action"))) decide(req, out, me);
        else create(req, out, me);
    }

    private void create(HttpServletRequest req, PrintWriter out, int me) {
        int itemId;
        try { itemId = Integer.parseInt(req.getParameter("itemId")); }
        catch (Exception e) { out.print("{\"success\":false,\"message\":\"Bad item\"}"); return; }

        String answer = req.getParameter("answer");
        if (answer == null || answer.trim().isEmpty()) {
            out.print("{\"success\":false,\"message\":\"Answer is required\"}");
            return;
        }

        try (Connection con = DBConnection.getConnection()) {

            // You cannot claim your own posting.
            try (PreparedStatement chk = con.prepareStatement(
                    "SELECT user_id FROM items WHERE item_id=?")) {
                chk.setInt(1, itemId);
                ResultSet rs = chk.executeQuery();
                if (!rs.next()) { out.print("{\"success\":false,\"message\":\"Item not found\"}"); return; }
                if (rs.getInt("user_id") == me) {
                    out.print("{\"success\":false,\"message\":\"You cannot claim your own item\"}");
                    return;
                }
            }

            try (PreparedStatement ps = con.prepareStatement(
                    "INSERT INTO claims (item_id,claimant_id,answer) VALUES (?,?,?)")) {
                ps.setInt(1, itemId);
                ps.setInt(2, me);
                ps.setString(3, answer.trim());
                ps.executeUpdate();
            }

            try (PreparedStatement up = con.prepareStatement(
                    "UPDATE items SET status='CLAIM_PENDING' WHERE item_id=? AND status='OPEN'")) {
                up.setInt(1, itemId);
                up.executeUpdate();
            }

            out.print("{\"success\":true,\"message\":\"Claim submitted — the finder will review it\"}");

        } catch (SQLIntegrityConstraintViolationException e) {
            out.print("{\"success\":false,\"message\":\"You have already claimed this item\"}");
        } catch (SQLException e) {
            e.printStackTrace();
            out.print("{\"success\":false,\"message\":\"Server error\"}");
        }
    }

    private void decide(HttpServletRequest req, PrintWriter out, int me) {
        int claimId;
        try { claimId = Integer.parseInt(req.getParameter("claimId")); }
        catch (Exception e) { out.print("{\"success\":false,\"message\":\"Bad claim\"}"); return; }

        String verdict = req.getParameter("verdict");
        if (!("APPROVED".equals(verdict) || "REJECTED".equals(verdict))) {
            out.print("{\"success\":false,\"message\":\"Bad verdict\"}");
            return;
        }

        try (Connection con = DBConnection.getConnection()) {

            int itemId;
            // Only the person who posted the item may decide on its claims.
            try (PreparedStatement chk = con.prepareStatement(
                    "SELECT i.item_id FROM claims c JOIN items i ON i.item_id=c.item_id "
                  + "WHERE c.claim_id=? AND i.user_id=?")) {
                chk.setInt(1, claimId);
                chk.setInt(2, me);
                ResultSet rs = chk.executeQuery();
                if (!rs.next()) { out.print("{\"success\":false,\"message\":\"Not allowed\"}"); return; }
                itemId = rs.getInt("item_id");
            }

            try (PreparedStatement ps = con.prepareStatement(
                    "UPDATE claims SET status=? WHERE claim_id=?")) {
                ps.setString(1, verdict);
                ps.setInt(2, claimId);
                ps.executeUpdate();
            }

            if ("APPROVED".equals(verdict)) {
                try (PreparedStatement a = con.prepareStatement(
                        "UPDATE items SET status='RESOLVED' WHERE item_id=?")) {
                    a.setInt(1, itemId);
                    a.executeUpdate();
                }
                try (PreparedStatement b = con.prepareStatement(
                        "UPDATE claims SET status='REJECTED' WHERE item_id=? AND claim_id<>? AND status='PENDING'")) {
                    b.setInt(1, itemId);
                    b.setInt(2, claimId);
                    b.executeUpdate();
                }
            }

            out.print("{\"success\":true,\"message\":\"Claim " + verdict.toLowerCase() + "\"}");

        } catch (SQLException e) {
            e.printStackTrace();
            out.print("{\"success\":false,\"message\":\"Server error\"}");
        }
    }
}