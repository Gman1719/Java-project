package controllers;

import java.net.URL;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ResourceBundle;

import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

public class AddUserController implements Initializable {

    // FXML fields defined in AddUser.fxml
    @FXML private TextField txtUsername;
    @FXML private PasswordField txtPassword;
    @FXML private TextField txtFirstName;
    @FXML private TextField txtLastName;
    @FXML private TextField txtEmail;
    @FXML private TextField txtPhone;
    @FXML private ComboBox<String> cbRole; 
    @FXML private ComboBox<String> cbDepartment;
    @FXML private TextField txtDesignation;
    @FXML private DatePicker dpDateJoined;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        // Load initial data for ComboBoxes (Roles and Departments)
        loadComboBoxData();
    }

    // --- Database Helper Methods ---

    /**
     * Loads the list of roles and departments into the ComboBoxes.
     */
    private void loadComboBoxData() {
        // ⭐ IMPORTANT: In a real application, fetch these from the database
        // For demonstration, we use sample data based on your schema inserts.
        
        // Roles (Admin, HR, Employee, Payroll Officer)
        cbRole.setItems(FXCollections.observableArrayList("Admin", "HR", "Employee", "Payroll Officer"));
        
        // Departments (Management, HR, IT, Finance)
        cbDepartment.setItems(FXCollections.observableArrayList("Management", "HR", "IT", "Finance"));
    }
    
    /**
     * Retrieves the role_id from the database based on the role name.
     */
    private int getRoleId(String roleName) throws SQLException {
        String sql = "SELECT role_id FROM roles WHERE role_name = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pst = conn.prepareStatement(sql)) {
            pst.setString(1, roleName);
            try (ResultSet rs = pst.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("role_id");
                }
            }
        }
        // Throw exception if role is not found, preventing a NULL foreign key.
        throw new SQLException("Role not found: " + roleName);
    }
    
    /**
     * Retrieves the dept_id from the database based on the department name.
     */
    private int getDeptId(String deptName) throws SQLException {
        String sql = "SELECT dept_id FROM departments WHERE dept_name = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pst = conn.prepareStatement(sql)) {
            pst.setString(1, deptName);
            try (ResultSet rs = pst.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("dept_id");
                }
            }
        }
        throw new SQLException("Department not found: " + deptName);
    }

    // --- FXML Event Handlers ---

    /**
     * Handles the 'Save User' button action. Inserts data into the 'users' table only.
     */
    @FXML
    private void handleSaveUser(ActionEvent event) {
        // Basic input validation
        if (txtUsername.getText().isEmpty() || txtPassword.getText().isEmpty() || cbRole.getValue() == null) {
            showAlert(AlertType.WARNING, "Input Error", "Username, Password, and Role are required fields.");
            return;
        }

        try {
            // 1. Get IDs using helper functions
            int roleId = getRoleId(cbRole.getValue());
            int deptId = getDeptId(cbDepartment.getValue());

            // 2. Prepare SQL for USERS table insertion
            String userSql = "INSERT INTO users (username, password, first_name, last_name, email, phone, dept_id, designation, date_of_joining, status, role_id) " +
                             "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
            
            try (Connection conn = DBConnection.getConnection();
                 PreparedStatement userPst = conn.prepareStatement(userSql)) {
                
                // 3. Bind Parameters (1 to 11)
                userPst.setString(1, txtUsername.getText().trim());
                userPst.setString(2, txtPassword.getText().trim()); // ⭐ In production, HASH this password!
                userPst.setString(3, txtFirstName.getText().trim());
                userPst.setString(4, txtLastName.getText().trim());
                userPst.setString(5, txtEmail.getText().trim());
                userPst.setString(6, txtPhone.getText().trim());
                userPst.setInt(7, deptId);
                userPst.setString(8, txtDesignation.getText().trim());
                
                // Handle Date of Joining (null check)
                if (dpDateJoined.getValue() != null) {
                    userPst.setDate(9, Date.valueOf(dpDateJoined.getValue()));
                } else {
                    userPst.setNull(9, java.sql.Types.DATE);
                }
                
                userPst.setString(10, "Active"); // Default status for a new user
                userPst.setInt(11, roleId);
                
                // 4. Execute the update
                userPst.executeUpdate();
                
                showAlert(AlertType.INFORMATION, "Success", "New System User '" + txtUsername.getText() + "' has been created successfully.");
                handleReset(event); // Clear form after successful save

            } // PreparedStatement and Connection close automatically
        } catch (SQLException e) {
            // Handle common SQL errors like duplicate username (UNIQUE constraint)
            if (e.getMessage().contains("Duplicate entry") && e.getMessage().contains("username")) {
                showAlert(AlertType.ERROR, "Database Error", "The username '" + txtUsername.getText() + "' already exists. Please choose a different one.");
            } else {
                e.printStackTrace();
                showAlert(AlertType.ERROR, "Database Error", "Failed to create user. Check the server logs for details.");
            }
        } catch (Exception e) {
            e.printStackTrace();
            showAlert(AlertType.ERROR, "Error", "An unexpected error occurred: " + e.getMessage());
        }
    }

    /**
     * Clears all fields in the form.
     */
    @FXML
    private void handleReset(ActionEvent event) {
        txtUsername.clear();
        txtPassword.clear();
        txtFirstName.clear();
        txtLastName.clear();
        txtEmail.clear();
        txtPhone.clear();
        txtDesignation.clear();
        cbRole.getSelectionModel().clearSelection();
        cbDepartment.getSelectionModel().clearSelection();
        dpDateJoined.setValue(null);
    }
    
    /**
     * Closes the current window/stage.
     */
    @FXML
    private void handleCancel(ActionEvent event) {
        // Get the current stage and close it
        Stage stage = (Stage) ((Button) event.getSource()).getScene().getWindow();
        stage.close();
    }
    
    /**
     * Placeholder for your application's alert method.
     */
    private void showAlert(AlertType type, String title, String message) {
        // Implementation of your standard JavaFX Alert box
        // Example: 
        // Alert alert = new Alert(type);
        // alert.setTitle(title);
        // alert.setHeaderText(null);
        // alert.setContentText(message);
        // alert.showAndWait();
    }
    
    // You would place your DBConnection class methods or reference here.
}