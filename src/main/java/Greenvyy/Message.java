package Greenvyy;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
//import java.util.ArrayList;
//import java.util.List;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
//import com.google.gson.Gson;

@WebServlet("/message")
public class Message extends HttpServlet {
    private static final long serialVersionUID = 1L;

    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String token = request.getHeader("Authorization"); // Récupère le token du header
        String content = request.getParameter("content");

       /* if (token == null || content == null || content.trim().isEmpty()) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write("Missing token or content.");
            return;
        }*/
        
        if (token == null || token.trim().isEmpty()) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write("Missing Authorization token.");
            return;
        }

        if (content == null || content.trim().isEmpty()) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write("Message content cannot be empty.");
            return;
        }

        try (Connection connection = DatabaseUtils.getConnection()) {
            // Vérifier si le token est valide
            String userQuery = "SELECT idmembre FROM membre WHERE auth_token = ?";
            try (PreparedStatement userStmt = connection.prepareStatement(userQuery)) {
                userStmt.setString(1, token);
                ResultSet rs = userStmt.executeQuery();

                if (rs.next()) {
                    int memberId = rs.getInt("idmembre");

                    // Publier le message
                    String messageQuery = "INSERT INTO messages (auteur, content) VALUES (?, ?)";
                    try (PreparedStatement messageStmt = connection.prepareStatement(messageQuery)) {
                        messageStmt.setInt(1, memberId);
                        messageStmt.setString(2, content);

                        int rowsInserted = messageStmt.executeUpdate();
                        if (rowsInserted > 0) {
                            response.getWriter().write("Message published successfully.");
                        } else {
                            response.getWriter().write("Failed to publish message.");
                        }
                    }
                } else {
                    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    response.getWriter().write("Invalid token.");
                }
            }
        } catch (SQLException e) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.getWriter().write("Database error: " + e.getMessage());
        }
    }
    
    // Méthode GET : Consulter les messages
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String token = request.getHeader("Authorization"); // Récupère le token du header

        if (token == null) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write("Missing token.");
            return;
        }

        try (Connection connection = DatabaseUtils.getConnection()) {
            // Vérifier si le token est valide
            String userQuery = "SELECT idmembre FROM membre WHERE auth_token = ?";
            try (PreparedStatement userStmt = connection.prepareStatement(userQuery)) {
                userStmt.setString(1, token);
                ResultSet rs = userStmt.executeQuery();

                if (rs.next()) {
                    // Récupérer les messages
                    String messageQuery = "SELECT m.content, mb.nom, mb.email, m.created_at " +
                                          "FROM messages m " +
                                          "JOIN membre mb ON m.auteur = mb.idmembre " +
                                          "ORDER BY m.created_at DESC";

                    try (PreparedStatement messageStmt = connection.prepareStatement(messageQuery)) {
                        ResultSet messages = messageStmt.executeQuery();
                        StringBuilder jsonResponse = new StringBuilder("[");
                        boolean first = true;

                        while (messages.next()) {
                            if (!first) {
                                jsonResponse.append(",");
                            }
                            jsonResponse.append("{");
                            jsonResponse.append("\"content\":\"").append(messages.getString("content")).append("\",");
                            jsonResponse.append("\"author\":\"").append(messages.getString("nom")).append(" (")
                            .append(messages.getString("email")).append(")\",");
                            jsonResponse.append("\"created_at\":\"").append(messages.getTimestamp("created_at")).append("\"");
                            jsonResponse.append("}");
                            first = false;
                        }
                        jsonResponse.append("]");

                        response.setContentType("application/json");
                        response.getWriter().write(jsonResponse.toString());
                    }
                } else {
                    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    response.getWriter().write("Invalid token.");
                }
            }
        } catch (SQLException e) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.getWriter().write("Database error: " + e.getMessage());
        }
    }
    
 // Méthode PUT : Modifier un message
    protected void doPut(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String token = request.getHeader("Authorization"); // Récupère le token
        String messageIdParam = request.getParameter("idmessage"); // ID du message à modifier
        String newContent = request.getParameter("content"); // Nouveau contenu du message

        if (token == null || messageIdParam == null || newContent == null || newContent.trim().isEmpty()) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write("Missing token, message ID, or new content.");
            return;
        }

        try {
            int messageId = Integer.parseInt(messageIdParam);

            try (Connection connection = DatabaseUtils.getConnection()) {
                // Vérifier si le token est valide
                String userQuery = "SELECT idmembre FROM membre WHERE auth_token = ?";
                try (PreparedStatement userStmt = connection.prepareStatement(userQuery)) {
                    userStmt.setString(1, token);
                    ResultSet rs = userStmt.executeQuery();

                    if (rs.next()) {
                        int memberId = rs.getInt("idmembre");

                        // Vérifier si le message appartient à l'utilisateur
                        String checkOwnershipQuery = "SELECT idmessage FROM messages WHERE idmessage = ? AND auteur = ?";
                        try (PreparedStatement ownershipStmt = connection.prepareStatement(checkOwnershipQuery)) {
                            ownershipStmt.setInt(1, messageId);
                            ownershipStmt.setInt(2, memberId);

                            ResultSet ownershipRs = ownershipStmt.executeQuery();
                            if (ownershipRs.next()) {
                                // Modifier le message
                                String updateQuery = "UPDATE messages SET content = ? WHERE idmessage = ?";
                                try (PreparedStatement updateStmt = connection.prepareStatement(updateQuery)) {
                                    updateStmt.setString(1, newContent);
                                    updateStmt.setInt(2, messageId);

                                    int rowsUpdated = updateStmt.executeUpdate();
                                    if (rowsUpdated > 0) {
                                        response.getWriter().write("Message updated successfully.");
                                    } else {
                                        response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                                        response.getWriter().write("Failed to update message.");
                                    }
                                }
                            } else {
                                response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                                response.getWriter().write("You are not authorized to update this message.");
                            }
                        }
                    } else {
                        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                        response.getWriter().write("Invalid token.");
                    }
                }
            }
        } catch (NumberFormatException e) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write("Invalid message ID format.");
        } catch (SQLException e) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.getWriter().write("Database error: " + e.getMessage());
        }
    }
    
 // Méthode DELETE : Supprimer un message
    protected void doDelete(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String token = request.getHeader("Authorization"); // Récupère le token
        String messageIdParam = request.getParameter("idmessage"); // ID du message à supprimer

        if (token == null || messageIdParam == null) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write("Missing token or message ID.");
            return;
        }

        try {
            int messageId = Integer.parseInt(messageIdParam);

            try (Connection connection = DatabaseUtils.getConnection()) {
                // Vérifier si le token est valide
                String userQuery = "SELECT idmembre FROM membre WHERE auth_token = ?";
                try (PreparedStatement userStmt = connection.prepareStatement(userQuery)) {
                    userStmt.setString(1, token);
                    ResultSet rs = userStmt.executeQuery();

                    if (rs.next()) {
                        int memberId = rs.getInt("idmembre");

                        // Vérifier si le message appartient à l'utilisateur
                        String checkOwnershipQuery = "SELECT idmessage FROM messages WHERE idmessage = ? AND auteur = ?";
                        try (PreparedStatement ownershipStmt = connection.prepareStatement(checkOwnershipQuery)) {
                            ownershipStmt.setInt(1, messageId);
                            ownershipStmt.setInt(2, memberId);

                            ResultSet ownershipRs = ownershipStmt.executeQuery();
                            if (ownershipRs.next()) {
                                // Supprimer le message
                                String deleteQuery = "DELETE FROM messages WHERE idmessage = ?";
                                try (PreparedStatement deleteStmt = connection.prepareStatement(deleteQuery)) {
                                    deleteStmt.setInt(1, messageId);

                                    int rowsDeleted = deleteStmt.executeUpdate();
                                    if (rowsDeleted > 0) {
                                        response.getWriter().write("Message deleted successfully.");
                                    } else {
                                        response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                                        response.getWriter().write("Failed to delete message.");
                                    }
                                }
                            } else {
                                response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                                response.getWriter().write("You are not authorized to delete this message.");
                            }
                        }
                    } else {
                        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                        response.getWriter().write("Invalid token.");
                    }
                }
            }
        } catch (NumberFormatException e) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write("Invalid message ID format.");
        } catch (SQLException e) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.getWriter().write("Database error: " + e.getMessage());
        }
    }
}