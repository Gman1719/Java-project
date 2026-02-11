// File: controllers/HRProfileController.java

package controllers;

import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.TextField;
import javafx.event.ActionEvent;

import java.sql.SQLException;
import java.util.regex.Pattern;

import services.UserService; 
import utils.SessionManager; 
import models.Employee;       

public class HRProfileController {

    @FXML private TextField txtFullName;
    @FXML private TextField txtEmail;
    @FXML private TextField txtPhone;

    private int currentUserId = -1;
    private UserService userService = new UserService(); 

    // --- Regex Patterns for Validation ---
    // Standard basic email validation
    private static final Pattern EMAIL_PATTERN = 
        Pattern.compile("^[\\w-\\.]+@([\\w-]+\\.)+[\\w-]{2,4}$");
        
    // Ethiopian Phone: Starts with 09 or +2519, followed by exactly 8 digits.
    private static final Pattern PHONE_PATTERN = 
        Pattern.compile("^(09|\\+2519)\\d{8}$");


    public void initialize() {
        currentUserId = SessionManager.getCurrentUserId();
        
        if (currentUserId > 0) {
            loadUserData(currentUserId);
        } else {
            showAlert(Alert.AlertType.ERROR, "Session Error", "User not logged in or session expired.");
            disableFields(true);
        }
    }

    private void loadUserData(int userId) {
        try {
            Employee user = userService.getUserProfileById(userId); 
            
            if (user != null) {
                // Populate fields using the combined getter
                txtFullName.setText(user.getFullName()); 
                txtEmail.setText(user.getEmail());
                txtPhone.setText(user.getPhone() != null ? user.getPhone() : ""); 
            } else {
                showAlert(Alert.AlertType.WARNING, "Data Error", "Could not find profile data for this user ID.");
                disableFields(true);
            }
        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Database Error", "Failed to load profile data.");
            e.printStackTrace();
            disableFields(true);
        }
    }

    @FXML
    private void handleSave(ActionEvent event) {
        if (currentUserId <= 0) {
            showAlert(Alert.AlertType.ERROR, "Session Error", "Cannot save changes without a valid user session.");
            return;
        }

        String fullName = txtFullName.getText().trim();
        String email = txtEmail.getText().trim();
        String phone = txtPhone.getText().trim();

        // 1. Validation Logic
        String validationError = validateFields(fullName, email, phone);
        if (validationError != null) {
            showAlert(Alert.AlertType.ERROR, "Validation Error", validationError);
            return;
        }

        // 2. Database Update Logic
        try {
            boolean success = userService.updateUserProfile(currentUserId, fullName, email, phone);
            
            if (success) {
                // ⭐️ FIX: Update the session manager using the new setFullName method
                SessionManager.getCurrentEmployee().setFullName(fullName); 
                SessionManager.getCurrentEmployee().setEmail(email);
                SessionManager.getCurrentEmployee().setPhone(phone);
                
                showAlert(Alert.AlertType.INFORMATION, "Success", "Profile updated successfully!");
            } else {
                 showAlert(Alert.AlertType.ERROR, "Update Failed", "Failed to update profile in the database. No rows affected.");
            }

        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Database Error", "Failed to save profile changes.");
            e.printStackTrace();
        }
    }
    
    // --- New Validation Helper Method ---
    private String validateFields(String fullName, String email, String phone) {
        if (fullName.isEmpty()) {
            return "Full Name is required.";
        }
        if (email.isEmpty()) {
            return "Email is required.";
        }
        if (!EMAIL_PATTERN.matcher(email).matches()) {
            return "Please enter a valid email address.";
        }
        if (!phone.isEmpty() && !PHONE_PATTERN.matcher(phone).matches()) {
            return "Phone number must be valid Ethiopian format (e.g., 0912345678 or +251912345678).";
        }
        // No errors
        return null;
    }


    @FXML
    private void handleCancel(ActionEvent event) {
        if (currentUserId > 0) {
            loadUserData(currentUserId);
            showAlert(Alert.AlertType.WARNING, "Action Cancelled", "Changes reverted to last saved state.");
        }
    }

    private void disableFields(boolean disable) {
         txtFullName.setDisable(disable);
         txtEmail.setDisable(disable);
         txtPhone.setDisable(disable);
         // The save button should also be disabled if fields are disabled, but FXML doesn't show it
    }

    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}