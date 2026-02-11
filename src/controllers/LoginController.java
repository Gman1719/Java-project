package controllers;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import models.Employee;
import utils.DBConnection;
import utils.SessionManager;

import java.sql.*;
import java.time.LocalDate;

public class LoginController {

    @FXML
    private TextField txtUsername;

    @FXML
    private PasswordField txtPassword;

    @FXML
    public void initialize() {
        // Database connectivity test
        try (Connection conn = DBConnection.getConnection()) {
            if (conn != null) {
                System.out.println("Database check successful.");
            } else {
                showAlert(Alert.AlertType.ERROR, "Database Error", "Failed to connect to database.");
            }
        } catch (SQLException e) {
            System.err.println("Database initialization failed: " + e.getMessage());
            showAlert(Alert.AlertType.ERROR, "Database Error", "Could not connect on startup.\n" + e.getMessage());
        }
    }

    @FXML
    private void handleLogin() {
        String username = txtUsername.getText().trim();
        String password = txtPassword.getText().trim();

        if (username.isEmpty() || password.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Login Failed", "Please enter both username and password!");
            return;
        }

        // --- STEP 1: AUTHENTICATION CHECK (Simple Query) ---
        // This query only checks the users table and role, ensuring it doesn't fail 
        // if the employee record is missing/incomplete for the admin user.
        String authSql =
                "SELECT u.user_id, r.role_name " +
                "FROM users u " +
                "JOIN roles r ON u.role_id = r.role_id " +
                "WHERE u.username = ? AND u.password = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pst = conn.prepareStatement(authSql)) {

            pst.setString(1, username);
            pst.setString(2, password);

            try (ResultSet rs = pst.executeQuery()) {
                if (rs.next()) {
                    int userId = rs.getInt("user_id");
                    String roleName = rs.getString("role_name");

                    // --- STEP 2: AUTHORIZATION AND DATA RETRIEVAL ---
                    // Fetch all details using a second method
                    Employee emp = fetchUserDetails(userId); 
                    
                    // The role_name from the initial auth check is more reliable
                    // than the one pulled from the complex join for basic Admin roles.
                    emp.setRoleName(roleName); 
                    
                    SessionManager.setCurrentEmployee(emp);
                    loadDashboard(emp.getRoleName());
                } else {
                    showAlert(Alert.AlertType.ERROR, "Login Failed", "Invalid username or password!");
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Database Error", e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "System Error", e.getMessage());
        }
    }

    /**
     * Fetches complete user/employee details based on user_id, 
     * handling cases where an Employee record might not exist 
     * (e.g., for a pure Admin user).
     * @param userId The validated user_id from the users table.
     * @return Employee object populated with all available data.
     */
    private Employee fetchUserDetails(int userId) throws SQLException {
        // This query includes all the LEFT JOINs from your original code
        String sql =
                "SELECT u.*, r.role_name, d.dept_name AS department, " +
                "e.position, e.date_joined, e.status, e.emp_id " +
                "FROM users u " +
                "JOIN roles r ON u.role_id = r.role_id " +
                "LEFT JOIN departments d ON u.dept_id = d.dept_id " +
                "LEFT JOIN employees e ON u.user_id = e.user_id " +
                "WHERE u.user_id = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pst = conn.prepareStatement(sql)) {

            pst.setInt(1, userId);

            try (ResultSet rs = pst.executeQuery()) {
                if (rs.next()) {
                    return createEmployeeFromResultSet(rs);
                }
            }
        }
        
        // This should not be reached if authentication succeeded, but required for compilation
        return new Employee(); 
    }
    
    // NOTE: The rest of the methods remain unchanged.

    private Employee createEmployeeFromResultSet(ResultSet rs) throws SQLException {
        Employee emp = new Employee();

        // emp_id might be NULL if the user is a non-employee Admin. getInt() handles NULL by returning 0.
        // If emp_id is 0, it means the user is a system user/non-payroll employee.
        emp.setEmployeeId(rs.getInt("emp_id")); 
        
        emp.setUserId(rs.getInt("user_id"));
        emp.setUsername(rs.getString("username"));
        emp.setFirstName(rs.getString("first_name"));
        emp.setLastName(rs.getString("last_name"));
        emp.setEmail(rs.getString("email"));
        emp.setPhone(rs.getString("phone"));
        
        // Department and Position will be NULL if no employee record, but this is handled by the model.
        emp.setDepartment(rs.getString("department"));
        emp.setPosition(rs.getString("position"));

        Date sqlDate = rs.getDate("date_joined");
        // Use a fallback date if date_joined is NULL (e.g., for non-payroll Admin)
        emp.setDateJoined(sqlDate != null ? sqlDate.toLocalDate() : LocalDate.of(1900, 1, 1)); 

        String status = rs.getString("status");
        emp.setStatus(status != null ? status : "Active");

        emp.setRoleId(rs.getInt("role_id"));
        emp.setRoleName(rs.getString("role_name"));

        return emp;
    }

    private void loadDashboard(String role) {
        // ... (The loadDashboard method is unchanged) ...
        try {
            Stage stage = (Stage) txtUsername.getScene().getWindow();
            String fxmlPath;
            String title;

            switch (role.toUpperCase()) {
                case "ADMIN" -> {
                    fxmlPath = "/views/admin_dashboard.fxml";
                    title = "Admin Dashboard";
                }
                case "HR" -> {
                    fxmlPath = "/views/hr_dashboard.fxml";
                    title = "HR Dashboard";
                }
                case "EMPLOYEE" -> {
                    fxmlPath = "/views/employee_dashboard.fxml";
                    title = "Employee Dashboard";
                }
                case "PAYROLL OFFICER" -> {
                    fxmlPath = "/views/accountant.fxml";
                    title = "Payroll Dashboard";
                }
                default -> {
                    showAlert(Alert.AlertType.ERROR, "Error", "Unknown role: " + role);
                    return;
                }
            }

            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Parent root = loader.load();
            stage.setScene(new Scene(root));
            stage.setTitle(title);
            stage.show();

        } catch (Exception e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Navigation Error", e.getMessage());
        }
    }

    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}