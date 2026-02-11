// utils/DBConnection.java (The Correct Simple Way)
package utils;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DBConnection {
    // These should be configured correctly
    private static final String URL = "jdbc:mysql://localhost:3306/payroll_system";
    private static final String USER = "root";
    private static final String PASSWORD = "root";

   
    public static Connection getConnection() throws SQLException {
        // No static field is used here. A new connection object is created.
        return DriverManager.getConnection(URL, USER, PASSWORD);
    }
}