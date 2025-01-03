package Greenvyy;

//Importation des dépendances
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DatabaseUtils {
	private static final String URL = "jdbc:mysql://localhost:3306/greenvy";
	private static final String USER = "greenvy";
	private static final String PASSWORD = "";


	public static Connection getConnection() throws SQLException {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            return DriverManager.getConnection(URL, USER, PASSWORD);
        } catch (ClassNotFoundException e) {
            throw new SQLException("MySQL JDBC driver not found", e);
        }
    }
}