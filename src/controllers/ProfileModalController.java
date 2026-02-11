package controllers;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import utils.DBConnection;
import java.io.File;
import java.net.URL;
import java.sql.*;
import java.util.ResourceBundle;
import utils.SessionManager;
import models.Employee;

public class ProfileModalController implements Initializable {

    @FXML private TextField txtFullName, txtUsername, txtEmail, txtPhone;
    @FXML private TextArea txtAddress;
    @FXML private ImageView profileImageView;
    @FXML private Button btnAction, btnUpload;

    private String selectedImagePath = null;
    private boolean isEditMode = false;
    private int currentUserId;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        // Dynamic ID from session
        this.currentUserId = SessionManager.getCurrentUserId();
        loadProfileData();
        setFieldsEditable(false);
    }

    private void loadProfileData() {
        String query = "SELECT u.username, u.first_name, u.last_name, u.email, u.phone, " +
                       "e.address, e.profile_picture_path " +
                       "FROM users u " +
                       "LEFT JOIN employees e ON u.user_id = e.user_id " +
                       "WHERE u.user_id = ?";
        
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(query)) {
            
            ps.setInt(1, currentUserId);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                txtUsername.setText(rs.getString("username"));
                String fName = rs.getString("first_name") == null ? "" : rs.getString("first_name");
                String lName = rs.getString("last_name") == null ? "" : rs.getString("last_name");
                txtFullName.setText((fName + " " + lName).trim());
                txtEmail.setText(rs.getString("email"));
                txtPhone.setText(rs.getString("phone"));
                
                String dbAddress = rs.getString("address");
                txtAddress.setText(dbAddress != null ? dbAddress : "");
                
                selectedImagePath = rs.getString("profile_picture_path");
                if (selectedImagePath != null && !selectedImagePath.isEmpty()) {
                    setProfileImage(selectedImagePath);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void setFieldsEditable(boolean editable) {
        txtFullName.setEditable(editable);
        txtEmail.setEditable(editable);
        txtPhone.setEditable(editable);
        txtAddress.setEditable(editable);
        
        if (btnAction != null) {
            btnAction.setText(editable ? "Save Changes" : "Edit Profile");
            btnAction.setStyle(editable ? "-fx-background-color: #2ecc71; -fx-text-fill: white; -fx-font-weight: bold;" 
                                       : "-fx-background-color: #3498db; -fx-text-fill: white; -fx-font-weight: bold;");
        }
    }

    @FXML
    private void handleUploadPhoto() {
        if (!isEditMode) {
            showAlert("Notice", "Please click 'Edit Profile' before changing photo.");
            return;
        }
        
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select Profile Image");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.jpeg"));
        
        File file = fileChooser.showOpenDialog(txtFullName.getScene().getWindow());
        if (file != null) {
            selectedImagePath = file.getAbsolutePath();
            setProfileImage(selectedImagePath);
        }
    }

    private void setProfileImage(String path) {
        try {
            File file = new File(path);
            if (file.exists()) {
                // Converting path to URI is critical for JavaFX to load local files
                Image image = new Image(file.toURI().toString());
                profileImageView.setImage(image);
            }
        } catch (Exception e) {
            System.err.println("Image Load Error: " + e.getMessage());
        }
    }

    @FXML
    private void handleToggleEdit() {
        if (!isEditMode) {
            isEditMode = true;
            setFieldsEditable(true);
        } else {
            saveToDatabase();
        }
    }

    private void saveToDatabase() {
        String fullName = txtFullName.getText().trim();
        if (fullName.isEmpty()) {
            showAlert("Error", "Name cannot be empty.");
            return;
        }

        String[] nameParts = fullName.split("\\s+", 2);
        String firstName = nameParts[0];
        String lastName = (nameParts.length > 1) ? nameParts[1] : "";

        try (Connection conn = DBConnection.getConnection()) {
            conn.setAutoCommit(false);

            // 1. Update USERS
            String sqlUser = "UPDATE users SET first_name=?, last_name=?, email=?, phone=? WHERE user_id=?";
            try (PreparedStatement psUser = conn.prepareStatement(sqlUser)) {
                psUser.setString(1, firstName);
                psUser.setString(2, lastName);
                psUser.setString(3, txtEmail.getText());
                psUser.setString(4, txtPhone.getText());
                psUser.setInt(5, currentUserId);
                psUser.executeUpdate();
            }

            // 2. UPSERT EMPLOYEES (Ensures record is created if it doesn't exist)
            String sqlEmp = "INSERT INTO employees (user_id, address, profile_picture_path, phone, email) " +
                            "VALUES (?, ?, ?, ?, ?) " +
                            "ON DUPLICATE KEY UPDATE address=?, profile_picture_path=?, phone=?, email=?";
            
            try (PreparedStatement psEmp = conn.prepareStatement(sqlEmp)) {
                psEmp.setInt(1, currentUserId);
                psEmp.setString(2, txtAddress.getText());
                psEmp.setString(3, selectedImagePath);
                psEmp.setString(4, txtPhone.getText());
                psEmp.setString(5, txtEmail.getText());
                // Update params
                psEmp.setString(6, txtAddress.getText());
                psEmp.setString(7, selectedImagePath);
                psEmp.setString(8, txtPhone.getText());
                psEmp.setString(9, txtEmail.getText());
                psEmp.executeUpdate();
            }

            conn.commit();

            // 3. SYNC SESSION
            Employee sessionEmp = SessionManager.getCurrentEmployee();
            if (sessionEmp != null) {
                sessionEmp.setFirstName(firstName);
                sessionEmp.setLastName(lastName);
                sessionEmp.setAddress(txtAddress.getText());
                sessionEmp.setProfilePicturePath(selectedImagePath);
                sessionEmp.setEmail(txtEmail.getText());
                sessionEmp.setPhone(txtPhone.getText());
            }

            isEditMode = false;
            setFieldsEditable(false);
            showAlert("Success", "Profile updated successfully!");

        } catch (SQLException e) {
            e.printStackTrace();
            showAlert("Database Error", "Check if your database connection is active.");
        }
    }

    @FXML
    private void handleClose() {
        ((Stage) txtFullName.getScene().getWindow()).close();
    }

    private void showAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}