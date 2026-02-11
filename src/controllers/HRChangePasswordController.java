package controllers;

import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.PasswordField;
import javafx.event.ActionEvent;
import java.sql.SQLException;

// CRITICAL: Update this import to match the location of your UserService class
import services.UserService; 
import utils.SessionManager; // <-- NEW IMPORT: For accessing the dynamic user ID

public class HRChangePasswordController {

    @FXML private PasswordField txtCurrentPassword;
    @FXML private PasswordField txtNewPassword;
    @FXML private PasswordField txtConfirmPassword;

    // Remove the hardcoded 'private int loggedInUserId = 1;'
    
    // Initialize the UserService
    private UserService userService = new UserService(); 

    @FXML
    private void handlePasswordUpdate(ActionEvent event) {
        // --- DYNAMICALLY GET USER ID FROM SESSION ---
        int loggedInUserId = SessionManager.getCurrentUserId(); 
        
        if (loggedInUserId <= 0) {
            showAlert(Alert.AlertType.ERROR, "Session Error", "User not logged in or session expired. Please log out and log back in.");
            return;
        }
        // ------------------------------------------

        String currentPass = txtCurrentPassword.getText();
        String newPass = txtNewPassword.getText();
        String confirmPass = txtConfirmPassword.getText();

        // 1. Validation Logic
        if (currentPass.isEmpty() || newPass.isEmpty() || confirmPass.isEmpty()) {
            showAlert(Alert.AlertType.ERROR, "Validation Error", "All password fields must be filled.");
            return;
        }
        if (!newPass.equals(confirmPass)) {
            showAlert(Alert.AlertType.ERROR, "Validation Error", "New Password and Confirm Password do not match.");
            return;
        }
        if (newPass.length() < 8) {
            showAlert(Alert.AlertType.ERROR, "Validation Error", "New Password must be at least 8 characters long.");
            return;
        }

        // 2. Database Interaction
        System.out.println("Attempting database password change for User ID: " + loggedInUserId);
        
        try {
            // STEP A: AUTHENTICATION - Verify current password
            if (!userService.verifyCurrentPasswordPlaintext(loggedInUserId, currentPass)) {
                showAlert(Alert.AlertType.ERROR, "Authentication Error", "The current password you entered is incorrect.");
                return;
            }

            // STEP B: UPDATE DATABASE - Store the new password
            boolean success = userService.updatePasswordPlaintext(loggedInUserId, newPass);

            if (success) {
                showAlert(Alert.AlertType.INFORMATION, "Success", "Password updated successfully and stored permanently.");
                
                // Clear fields on success
                txtCurrentPassword.clear();
                txtNewPassword.clear();
                txtConfirmPassword.clear();
            } else {
                showAlert(Alert.AlertType.ERROR, "Database Error", "Failed to update the password in the database. No rows were affected.");
            }

        } catch (SQLException e) {
            System.err.println("Database Error during password update: " + e.getMessage());
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Database Error", "A database connection error occurred. Check console for details.");
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