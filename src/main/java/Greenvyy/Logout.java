package Greenvyy;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

@WebServlet("/logout")
public class Logout extends HttpServlet {
    private static final long serialVersionUID = 1L;

    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String authToken = request.getHeader("Authorization");

        if (authToken == null || authToken.trim().isEmpty()) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write("Authorization token is required.");
            return;
        }

        try (Connection connection = DatabaseUtils.getConnection()) {
            // Requête pour supprimer le token de la base de données
            String query = "UPDATE membre SET auth_token = NULL WHERE auth_token = ?";
            try (PreparedStatement stmt = connection.prepareStatement(query)) {
                stmt.setString(1, authToken);
                int rowsUpdated = stmt.executeUpdate();

                if (rowsUpdated > 0) {
                    response.getWriter().write("User logged out successfully.");
                } else {
                    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    response.getWriter().write("Invalid token.");
                }
            }
        } catch (SQLException e) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.getWriter().write("Error: " + e.getMessage());
        }
    }
}