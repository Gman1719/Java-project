package controllers;

import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.TextField;
import javafx.scene.control.TextArea;
import javafx.scene.control.Label;
import javafx.stage.Stage;
import models.Department;
import utils.DBConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class DepartmentFormController {

    // --- FXML UI Elements ---
    @FXML private Label lblTitle;
    @FXML private TextField tfDeptName;
    @FXML private TextArea taDescription;

    // --- Internal State ---
    private Department editingDepartment = null; // Stores the department being edited (null for Add)
    private DepartmentController parentController; // Reference to the main view controller

    /**
     * Called by the parent controller to pass the department object and set the form mode.
     * @param dept The Department object to edit, or null for a new department.
     */
    public void setDepartment(Department dept) {
        this.editingDepartment = dept;
        
        if (dept != null) {
            // EDIT Mode
            lblTitle.setText("Edit Department");
            tfDeptName.setText(dept.getName());
            taDescription.setText(dept.getDescription());
        } else {
            // ADD Mode
            lblTitle.setText("Add New Department");
        }
    }

    /**
     * Called by the parent controller to establish a link for refreshing the main table.
     * @param controller The instance of DepartmentController.
     */
    public void setParentController(DepartmentController controller) {
        this.parentController = controller;
    }

    // --- FXML Action Handlers ---

    @FXML
    private void saveDepartment() {
        String name = tfDeptName.getText().trim();
        String description = taDescription.getText().trim();

        if (name.isEmpty()) {
            showAlert("Validation Error", "Department Name cannot be empty.", Alert.AlertType.WARNING);
            return;
        }

        try (Connection conn = DBConnection.getConnection()) {
            if (editingDepartment == null) {
                // Handle INSERT (CREATE)
                insertDepartment(conn, name, description);
            } else {
                // Handle UPDATE (EDIT)
                updateDepartment(conn, name, description);
            }
            
            // 1. Inform the user
            showAlert("Success", "Department data saved successfully!", Alert.AlertType.INFORMATION);
            
            // 2. Refresh the main list
            if (parentController != null) {
                // Calls the public method in the parent controller
                parentController.loadDepartments(); 
            }
            
            // 3. Close the modal window
            closeForm();
            
        } catch (SQLException e) {
            e.printStackTrace();
            showAlert("Database Error", "Failed to save department: " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    private void insertDepartment(Connection conn, String name, String description) throws SQLException {
        String sql = "INSERT INTO departments (dept_name, description) VALUES (?, ?)";
        try (PreparedStatement pst = conn.prepareStatement(sql)) {
            pst.setString(1, name);
            pst.setString(2, description);
            pst.executeUpdate();
        }
    }

    private void updateDepartment(Connection conn, String name, String description) throws SQLException {
        String sql = "UPDATE departments SET dept_name = ?, description = ? WHERE dept_id = ?";
        try (PreparedStatement pst = conn.prepareStatement(sql)) {
            pst.setString(1, name);
            pst.setString(2, description);
            pst.setInt(3, editingDepartment.getDeptId());
            pst.executeUpdate();
        }
    }

    @FXML
    private void closeForm() {
        // Gets the current window (Stage) associated with an FXML element and closes it
        Stage stage = (Stage) tfDeptName.getScene().getWindow();
        stage.close();
    }

    // --- Utility Methods ---

    private void showAlert(String title, String content, Alert.AlertType type) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}