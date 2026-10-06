package com.foundit;

import java.io.IOException;
import java.io.PrintWriter;
import java.sql.*;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;

@WebServlet("/auth")
public class AuthServlet extends HttpServlet {

    protected void doGet(HttpServletRequest req, HttpServletResponse res)
            throws ServletException, IOException {
        res.setContentType("application/json");
        PrintWriter out = res.getWriter();
        String action = req.getParameter("action");
        HttpSession s = req.getSession(false);

        if ("me".equals(action)) {
            if (s != null && s.getAttribute("userId") != null) {
                out.print("{\"success\":true,\"userId\":" + s.getAttribute("userId")
                        + ",\"name\":\"" + DBConnection.esc((String) s.getAttribute("userName")) + "\"}");
            } else {
                out.print("{\"success\":false}");
            }
        } else if ("logout".equals(action)) {
            if (s != null) s.invalidate();
            out.print("{\"success\":true}");
        } else {
            out.print("{\"success\":false,\"message\":\"Unknown action\"}");
        }
    }

    protected void doPost(HttpServletRequest req, HttpServletResponse res)
            throws ServletException, IOException {
        res.setContentType("application/json");
        PrintWriter out = res.getWriter();
        String action = req.getParameter("action");

        if ("register".equals(action)) register(req, out);
        else if ("login".equals(action)) login(req, out);
        else out.print("{\"success\":false,\"message\":\"Unknown action\"}");
    }

    private void register(HttpServletRequest req, PrintWriter out) {
        String name  = req.getParameter("name");
        String email = req.getParameter("email");
        String pass  = req.getParameter("password");

        if (name == null || email == null || pass == null
                || name.trim().isEmpty() || email.trim().isEmpty() || pass.length() < 6) {
            out.print("{\"success\":false,\"message\":\"All fields required, password min 6 chars\"}");
            return;
        }

        String sql = "INSERT INTO users (name, email, password) VALUES (?,?,?)";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, name.trim());
            ps.setString(2, email.trim().toLowerCase());
            ps.setString(3, PasswordUtil.hash(pass));
            ps.executeUpdate();
            out.print("{\"success\":true,\"message\":\"Registered — now log in\"}");
        } catch (SQLIntegrityConstraintViolationException e) {
            out.print("{\"success\":false,\"message\":\"Email already registered\"}");
        } catch (SQLException e) {
            e.printStackTrace();
            out.print("{\"success\":false,\"message\":\"Server error\"}");
        }
    }

    private void login(HttpServletRequest req, PrintWriter out) {
        String email = req.getParameter("email");
        String pass  = req.getParameter("password");

        String sql = "SELECT user_id, name, password FROM users WHERE email=?";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, email == null ? "" : email.trim().toLowerCase());
            ResultSet rs = ps.executeQuery();

            if (rs.next() && PasswordUtil.verify(pass == null ? "" : pass,
                                                 rs.getString("password"))) {
                HttpSession s = req.getSession(true);
                s.setAttribute("userId", rs.getInt("user_id"));
                s.setAttribute("userName", rs.getString("name"));
                out.print("{\"success\":true}");
            } else {
                out.print("{\"success\":false,\"message\":\"Invalid email or password\"}");
            }
        } catch (SQLException e) {
            e.printStackTrace();
            out.print("{\"success\":false,\"message\":\"Server error\"}");
        }
    }
}