package Greenvyy;

import java.sql.Connection;
import java.sql.PreparedStatement;
//import java.sql.ResultSet;
import java.sql.SQLException;
//import java.util.UUID;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
//import jakarta.servlet.http.HttpSession;
import java.io.IOException;

/**
 * Servlet implementation class register
 */
@WebServlet("/register")
public class Register extends HttpServlet {
    private static final long serialVersionUID = 1L;

    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
    	String nom = request.getParameter("nom");
    	String email = request.getParameter("email"); // Récupérer l'adresse mail
        String password = request.getParameter("password"); // Récupérer le mot de passe
        String expjardinage = request.getParameter("expjardinage");
        String plantesprefer  = request.getParameter("plantesprefer");

        if (nom != null && email != null && password != null) { // Vérification que les paramètres sont présents

            // Connexion à la base de données pour enregistrer l'utilisateur
            try (Connection connection = DatabaseUtils.getConnection()) {
                // Requête SQL pour insérer un nouvel utilisateur dans la base de données
                String query = "INSERT INTO membre (nom, email, password, expjardinage, plantesprefer) VALUES (?, ?, ?, ?, ?)";
                try (PreparedStatement stmt = connection.prepareStatement(query)) {
                	stmt.setString(1, nom);
                	stmt.setString(2, email); // Lier l'adresse mail à la deuxième valeur  de la requête
                	stmt.setString(3, hashPassword(password)); // Lier le mot de passe en clair à la troisième valeur
                    stmt.setString(4, expjardinage);
                    stmt.setString(5, plantesprefer);
                    int rowsInserted = stmt.executeUpdate(); // Exécuter la requête

                    if (rowsInserted > 0) {
                        response.getWriter().write("User registered successfully.");
                    } else {
                        response.getWriter().write("Failed to register user.");
                    }
                }
            } catch (SQLException e) {
                response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR); // 500 Internal Server Error
                response.getWriter().write("SQL Error: " + e.getMessage()); // Afficher l'erreur SQL dans la réponse
                e.printStackTrace(); // Afficher la trace de l'exception dans la console
            } catch (Exception e) {
                response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR); // 500 Internal Server Error
                response.getWriter().write("Unexpected Error: " + e.getMessage()); // Afficher l'erreur générale
                e.printStackTrace(); // Afficher la trace de l'exception dans la console
            }
        } else {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST); // 400 Bad Request
            response.getWriter().write("Missing nom, email, or password.");
        }
    }
    private String hashPassword(String password) throws NoSuchAlgorithmException {
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        byte[] hash = md.digest(password.getBytes());
        StringBuilder hexString = new StringBuilder();
        for (byte b : hash) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) {
                hexString.append('0');
            }
            hexString.append(hex);
        }
        return hexString.toString();
    }
}