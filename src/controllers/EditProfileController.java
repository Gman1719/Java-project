package controllers;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.regex.Pattern;

import javafx.scene.control.Alert.AlertType;
import utils.DBConnection; 
import utils.ProfileUpdateListener; 

public class EditProfileController {

    // ⭐ This is the user ID passed from the dashboard
    private int userId; 
    
    // Listener to notify the parent window (Dashboard) to refresh after saving
    private ProfileUpdateListener listener; 

    // FXML fields linked to the provided FXML
    @FXML private TextField txtFirstName;
    @FXML private TextField txtLastName;
    @FXML private Label lblEmail;
    @FXML private TextField txtPhone;
    @FXML private TextArea txtAddress;
    @FXML private Label lblDateJoined;
    @FXML private Label lblPosition;
    
    // Regex for phone number validation
    private static final Pattern PHONE_PATTERN = 
        Pattern.compile("^(09|\\+2519)\\d{8}$");

    /**
     * Called by the launching controller (Dashboard) to set the user context.
     */
    public void setUserId(int userId) {
        this.userId = userId;
        loadProfileData();
    }
    
    /**
     * Setter for the ProfileUpdateListener.
     */
    public void setProfileUpdateListener(ProfileUpdateListener listener) {
        this.listener = listener;
    }

    @FXML
    private void initialize() {
        // Initialization code
    }

    /**
     * Loads the current user's profile data from the database.
     */
    private void loadProfileData() {
        if (userId == 0) {
             System.err.println("Cannot load profile data: userId is 0.");
             return; 
        }

        // SQL: Joins users (for names, email) and employees (for phone, address, position, date_joined)
        String sql = "SELECT u.first_name, u.last_name, u.email, e.phone, e.address, e.date_joined, e.position " +
                     "FROM users u JOIN employees e ON u.user_id = e.user_id WHERE u.user_id = ?";
        
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pst = conn.prepareStatement(sql)) {
            
            pst.setInt(1, userId);
            
            try (ResultSet rs = pst.executeQuery()) {
                if (rs.next()) {
                    txtFirstName.setText(rs.getString("first_name"));
                    txtLastName.setText(rs.getString("last_name"));
                    lblEmail.setText(rs.getString("email"));
                    
                    // Fields from employees table (with null checks)
                    txtPhone.setText(rs.getString("phone") != null ? rs.getString("phone") : "");
                    txtAddress.setText(rs.getString("address") != null ? rs.getString("address") : "");
                    lblDateJoined.setText(rs.getDate("date_joined").toString());
                    lblPosition.setText(rs.getString("position")); 
                }
            }
        } catch (SQLException e) {
            showAlert(AlertType.ERROR, "Database Error", "Failed to load profile data.");
            e.printStackTrace();
        }
    }
    
    /**
     * Performs client-side validation for fields before saving.
     */
    private boolean validateInput() {
        String phone = txtPhone.getText().trim();
        
        if (txtFirstName.getText().trim().isEmpty() || 
            txtLastName.getText().trim().isEmpty() || 
            phone.isEmpty()) {
            showAlert(AlertType.WARNING, "Validation Failed", "First Name, Last Name, and Phone Number cannot be empty.");
            return false;
        }

        if (!PHONE_PATTERN.matcher(phone).matches()) {
            showAlert(AlertType.WARNING, "Validation Failed", "Phone Number format must start with '09' or '+2519' and be followed by 8 digits.");
            return false;
        }
        return true;
    }

    /**
     * Handles the "Save Changes" button action using a database transaction.
     */
    @FXML
    public void handleSaveProfile() {
        if (!validateInput()) {
            return; 
        }
        
        Connection conn = null; 
        
        // 1. Query to update USERS table (first_name, last_name)
        String updateUsersSql = "UPDATE users SET first_name = ?, last_name = ? WHERE user_id = ?";
        // 2. Query to update EMPLOYEES table (phone, address)
        String updateEmployeesSql = "UPDATE employees SET phone = ?, address = ? WHERE user_id = ?";
        
        try {
            conn = DBConnection.getConnection();
            
            if (conn == null) {
                showAlert(AlertType.ERROR, "Connection Error", "Database connection failed. Check DBConnection class.");
                return;
            }

            conn.setAutoCommit(false); // START TRANSACTION
            
            // EXECUTE 1: Update USERS table
            int usersRowsUpdated = 0;
            try (PreparedStatement pstUser = conn.prepareStatement(updateUsersSql)) {
                pstUser.setString(1, txtFirstName.getText().trim());
                pstUser.setString(2, txtLastName.getText().trim());
                pstUser.setInt(3, userId);
                usersRowsUpdated = pstUser.executeUpdate();
            }
            
            // EXECUTE 2: Update EMPLOYEES table (Phone and Address only)
            int employeesRowsUpdated = 0;
            try (PreparedStatement pstEmp = conn.prepareStatement(updateEmployeesSql)) {
                pstEmp.setString(1, txtPhone.getText().trim());
                pstEmp.setString(2, txtAddress.getText().trim());
                pstEmp.setInt(3, userId);
                employeesRowsUpdated = pstEmp.executeUpdate();
            }
            
            // Commit only if records were found in both tables
            if (usersRowsUpdated > 0 && employeesRowsUpdated > 0) {
                conn.commit(); // COMMIT TRANSACTION
                showAlert(AlertType.INFORMATION, "Success", "Profile updated successfully!");
                
                // Notify the parent controller to reload its displayed data
                if (listener != null) {
                    listener.onProfileUpdated();
                }
                
                handleCancel(); 
            } else {
                 conn.rollback();
                 String missingTable = (usersRowsUpdated == 0) ? "USERS" : "EMPLOYEES";
                 showAlert(AlertType.ERROR, "Update Failed", 
                           "Update aborted. Could not find user record (ID: " + userId + ") in " + missingTable + " table.");
            }

        } catch (SQLException e) {
            // ROLLBACK on error
            try {
                if (conn != null) {
                    conn.rollback();
                }
            } catch (SQLException rollbackEx) {
                rollbackEx.printStackTrace();
            }
            showAlert(AlertType.ERROR, "Update Failed", "Database error during profile update. Changes rolled back. Details: " + e.getMessage());
            e.printStackTrace();
            
        } finally {
            if (conn != null) {
                try {
                    conn.close();
                } catch (SQLException closeEx) {
                    closeEx.printStackTrace();
                }
            }
        }
    }

    /**
     * Handles the "Cancel" button action.
     */
    @FXML
    public void handleCancel() {
        // Closes the current Stage (the Edit Profile window)
        Stage stage = (Stage) txtFirstName.getScene().getWindow();
        stage.close();
    }

    private void showAlert(AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}