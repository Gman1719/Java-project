package controllers;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import javafx.scene.control.Alert.AlertType;
import utils.DBConnection; 


public class ChangePasswordController {

    private int userId; 

    @FXML private PasswordField pfCurrentPassword;
    @FXML private PasswordField pfNewPassword;
    @FXML private PasswordField pfConfirmPassword;

    public void setUserId(int userId) {
        this.userId = userId;
    }

    @FXML
    private void initialize() {
        // Initialization logic
    }

    @FXML
    public void handleUpdatePassword() {
        // Trim inputs immediately to prevent issues with user-entered spaces
        String currentPass = pfCurrentPassword.getText().trim();
        String newPass = pfNewPassword.getText().trim();
        String confirmPass = pfConfirmPassword.getText().trim();
        
        if (!validateInput(currentPass, newPass, confirmPass)) { return; }
        
        if (!verifyCurrentPassword(currentPass)) {
            showAlert(AlertType.ERROR, "Authentication Failed", "The current password you entered is incorrect.");
            return;
        }

        if (updateNewPassword(newPass)) {
            showAlert(AlertType.INFORMATION, "Success", "Your password has been updated successfully!");
            // Close the window after successful update
            handleCancel(); 
        } else {
            showAlert(AlertType.ERROR, "Update Failed", "A database error occurred while updating the password.");
        }
    }
    
    private boolean validateInput(String currentPass, String newPass, String confirmPass) {
        // Check if fields are empty (after trimming)
        if (currentPass.isEmpty() || newPass.isEmpty() || confirmPass.isEmpty()) {
            showAlert(AlertType.WARNING, "Missing Data", "Please fill in all password fields.");
            return false;
        }
        if (!newPass.equals(confirmPass)) {
            showAlert(AlertType.ERROR, "Mismatch", "New password and confirmation do not match.");
            return false;
        }
        if (newPass.length() < 8) {
            showAlert(AlertType.WARNING, "Weak Password", "New password must be at least 8 characters long.");
            return false;
        }
        if (newPass.equals(currentPass)) {
             showAlert(AlertType.WARNING, "Same Password", "New password cannot be the same as the current password.");
            return false;
        }
        return true;
    }

    /**
     * Verifies the user's current password against the database record.
     * Includes trimming of the stored password to handle potential database data issues.
     */
    private boolean verifyCurrentPassword(String currentPass) {
        String storedPass = null;
        
        // --- ADDED DEBUGGING LINE ---
        System.out.println("DEBUG: Verifying password for User ID: " + userId);
        
        String sql = "SELECT password FROM users WHERE user_id = ?";
        
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pst = conn.prepareStatement(sql)) {
            
            pst.setInt(1, userId);
            try (ResultSet rs = pst.executeQuery()) {
                if (rs.next()) {
                    String dbPasswordRaw = rs.getString("password");
                    if (dbPasswordRaw != null) {
                        storedPass = dbPasswordRaw.trim();
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
        
        String trimmedCurrentPass = currentPass.trim();
        
        if (storedPass == null) {
             System.out.println("DEBUG: User ID " + userId + " found, but password column was NULL or empty.");
             return false;
        }
        
        // --- ADDED DEBUGGING LINE ---
        System.out.println("DEBUG: Input Pass (trimmed): [" + trimmedCurrentPass + "]");
        System.out.println("DEBUG: Stored Pass (trimmed): [" + storedPass + "]");

        return trimmedCurrentPass.equals(storedPass); 
    }
    private boolean updateNewPassword(String newPass) {
        // Update using the assumed correct column name: 'password'
        String sql = "UPDATE users SET password = ? WHERE user_id = ?";
        
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pst = conn.prepareStatement(sql)) {
            
            // Storing the new password string directly
            pst.setString(1, newPass); 
            pst.setInt(2, userId);
            return pst.executeUpdate() > 0;
            
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    @FXML
    public void handleCancel() {
        // Close the current window
        Stage stage = (Stage) pfCurrentPassword.getScene().getWindow();
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