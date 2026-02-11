package controllers;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;
import models.Employee; // Assuming this model holds combined user/employee data
import utils.DBConnection;

import java.sql.*;
import java.util.HashMap;
import java.util.Map;

// NOTE: This controller assumes it is used by both the Add and Edit forms.
public class EmployeeFormController {

    // --- FXML UI Elements ---
    @FXML private Label lblTitle;
    @FXML private TextField tfUsername, tfPassword, tfFirstName, tfLastName, tfEmail, tfPhone;
    @FXML private TextField tfDesignation, tfPosition, tfSalary, tfBankAccount;
    // NOTE: Department is a Foreign Key in the DB, should be a ComboBox here.
    @FXML private ComboBox<String> cbRole, cbDepartment; 
    @FXML private DatePicker dpDateOfJoining;
    @FXML private ComboBox<String> cbStatus, cbGender;

    // --- Internal State ---
    private Employee editingEmployee = null; // null means Add new
    private Map<String, Integer> roleMap = new HashMap<>(); // role_name -> role_id
    private Map<String, Integer> deptMap = new HashMap<>(); // dept_name -> dept_id
    private UserManagementController parentController; // Reference to refresh the list

    /**
     * Initializes the form components and loads dynamic data.
     */
    public void initialize() {
        lblTitle.setText("Add New Employee");
        
        // Populate static status/gender options
        cbStatus.setItems(FXCollections.observableArrayList("Active", "Inactive"));
        cbGender.setItems(FXCollections.observableArrayList("Male", "Female", "Other"));
        
        // Load dynamic data from DB
        try {
            loadRolesAndDepartments();
        } catch (SQLException e) {
            e.printStackTrace();
            showAlert("Database Error", "Failed to load roles or departments.", Alert.AlertType.ERROR);
        }

        // Set initial values
        cbStatus.setValue("Active");
        // tfPassword must be set for new users but disabled for edits
    }

    /**
     * Loads role and department IDs into maps and populates ComboBoxes.
     */
    private void loadRolesAndDepartments() throws SQLException {
        try (Connection conn = DBConnection.getConnection()) {
            // Load Roles
            PreparedStatement psRoles = conn.prepareStatement("SELECT role_id, role_name FROM roles");
            ResultSet rsRoles = psRoles.executeQuery();
            ObservableList<String> roleNames = FXCollections.observableArrayList();
            while (rsRoles.next()) {
                roleNames.add(rsRoles.getString("role_name"));
                roleMap.put(rsRoles.getString("role_name"), rsRoles.getInt("role_id"));
            }
            cbRole.setItems(roleNames);

            // Load Departments
            PreparedStatement psDepts = conn.prepareStatement("SELECT dept_id, dept_name FROM departments");
            ResultSet rsDepts = psDepts.executeQuery();
            ObservableList<String> deptNames = FXCollections.observableArrayList();
            while (rsDepts.next()) {
                deptNames.add(rsDepts.getString("dept_name"));
                deptMap.put(rsDepts.getString("dept_name"), rsDepts.getInt("dept_id"));
            }
            cbDepartment.setItems(deptNames);
        }
    }

    /**
     * Sets the Employee model for editing and populates form fields.
     */
    public void setEmployee(Employee emp) {
        this.editingEmployee = emp;
        if (emp != null) {
            lblTitle.setText("Edit User/Employee");
            
            // Disable username/password editing for security and integrity
            tfUsername.setDisable(true);
            tfPassword.setDisable(true); 
            
            // User Data
            tfUsername.setText(emp.getUsername());
            tfFirstName.setText(emp.getFirstName());
            tfLastName.setText(emp.getLastName());
            tfEmail.setText(emp.getEmail());
            tfPhone.setText(emp.getPhone());
            cbRole.setValue(emp.getRoleName());
            cbStatus.setValue(emp.getStatus());
            
            // Employee Data (if applicable)
            cbGender.setValue(emp.getGender());
            cbDepartment.setValue(emp.getDepartmentName()); // Assuming Employee model has this getter
            tfDesignation.setText(emp.getDesignation());
            tfPosition.setText(emp.getPosition());
            tfSalary.setText(String.valueOf(emp.getSalary()));
            tfBankAccount.setText(emp.getBankAccount());
            dpDateOfJoining.setValue(emp.getDateJoined());
        }
    }
    
    /**
     * Sets a reference to the calling controller to enable table refresh.
     */
    public void setParentController(UserManagementController controller) {
        this.parentController = controller;
    }

    // ------------------ SAVE LOGIC ------------------

    @FXML
    private void saveEmployee() {
        if (!validateInput()) return;

        try (Connection conn = DBConnection.getConnection()) {
            conn.setAutoCommit(false); // Start transaction for atomicity

            if (editingEmployee == null) {
                // 1. INSERT into USERS table
                int userId = insertNewUser(conn);
                
                // 2. INSERT into EMPLOYEES table (linking to the new user_id)
                insertNewEmployee(conn, userId);
            } else {
                // 1. UPDATE USERS table
                updateExistingUser(conn);
                
                // 2. UPDATE EMPLOYEES table
                updateExistingEmployee(conn);
            }

            conn.commit(); // Commit transaction

            // Inform and refresh
            showAlert("Success", "User data saved successfully!", Alert.AlertType.INFORMATION);
            if (parentController != null) {
                // Assuming the parent controller has a public method to refresh the view
                parentController.loadUsers(); 
            }
            closeForm();

        } catch (SQLException e) {
            try {
                // Rollback on error
                DBConnection.getConnection().rollback();
            } catch (SQLException rollbackEx) {
                rollbackEx.printStackTrace();
            }
            e.printStackTrace();
            showAlert("Database Error", "Failed to save user data. Please check logs.", Alert.AlertType.ERROR);
        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Error", "An unexpected error occurred: " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    // ------------------ TRANSACTIONAL METHODS ------------------

    private int insertNewUser(Connection conn) throws SQLException {
        // ⭐ IMPORTANT: This should ONLY run if editingEmployee is null
        String passwordHash = tfPassword.getText(); // Should be hashed in a real app!
        Integer roleId = roleMap.get(cbRole.getValue());
        Integer deptId = deptMap.get(cbDepartment.getValue());
        
        String sql =
            "INSERT INTO users (username, password, first_name, last_name, email, phone, role_id, dept_id, designation, status, date_of_joining) " +
            "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, tfUsername.getText());
            ps.setString(2, passwordHash);
            ps.setString(3, tfFirstName.getText());
            ps.setString(4, tfLastName.getText());
            ps.setString(5, tfEmail.getText());
            ps.setString(6, tfPhone.getText());
            ps.setInt(7, roleId);
            ps.setInt(8, deptId);
            ps.setString(9, tfDesignation.getText());
            ps.setString(10, cbStatus.getValue());
            ps.setDate(11, Date.valueOf(dpDateOfJoining.getValue()));
            ps.executeUpdate();
            
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    return rs.getInt(1); // Return the generated user_id
                } else {
                    throw new SQLException("Failed to retrieve generated user_id.");
                }
            }
        }
    }
    
    private void insertNewEmployee(Connection conn, int userId) throws SQLException {
        Integer roleId = roleMap.get(cbRole.getValue());
        Integer deptId = deptMap.get(cbDepartment.getValue());
        
        // Insert into employees table
        String sql =
            "INSERT INTO employees (user_id, role_id, gender, phone, email, dept_id, position, salary, bank_account, date_joined, status) " +
            "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
            
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setInt(2, roleId);
            ps.setString(3, cbGender.getValue());
            ps.setString(4, tfPhone.getText());
            ps.setString(5, tfEmail.getText());
            ps.setInt(6, deptId);
            ps.setString(7, tfPosition.getText());
            ps.setDouble(8, Double.parseDouble(tfSalary.getText()));
            ps.setString(9, tfBankAccount.getText());
            ps.setDate(10, Date.valueOf(dpDateOfJoining.getValue()));
            ps.setString(11, cbStatus.getValue());
            ps.executeUpdate();
        }
    }
    
    private void updateExistingUser(Connection conn) throws SQLException {
        Integer roleId = roleMap.get(cbRole.getValue());
        Integer deptId = deptMap.get(cbDepartment.getValue());

        String sql =
            "UPDATE users SET first_name=?, last_name=?, email=?, phone=?, role_id=?, dept_id=?, designation=?, status=? " +
            "WHERE user_id=?";
            
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, tfFirstName.getText());
            ps.setString(2, tfLastName.getText());
            ps.setString(3, tfEmail.getText());
            ps.setString(4, tfPhone.getText());
            ps.setInt(5, roleId);
            ps.setInt(6, deptId);
            ps.setString(7, tfDesignation.getText());
            ps.setString(8, cbStatus.getValue());
            ps.setInt(9, editingEmployee.getUserId());
            ps.executeUpdate();
        }
    }
    
    private void updateExistingEmployee(Connection conn) throws SQLException {
        Integer roleId = roleMap.get(cbRole.getValue());
        Integer deptId = deptMap.get(cbDepartment.getValue());

        String sql =
            "UPDATE employees SET role_id=?, gender=?, phone=?, email=?, dept_id=?, position=?, salary=?, bank_account=?, date_joined=?, status=? " +
            "WHERE emp_id=?";
            
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, roleId);
            ps.setString(2, cbGender.getValue());
            ps.setString(3, tfPhone.getText());
            ps.setString(4, tfEmail.getText());
            ps.setInt(5, deptId);
            ps.setString(6, tfPosition.getText());
            ps.setDouble(7, Double.parseDouble(tfSalary.getText()));
            ps.setString(8, tfBankAccount.getText());
            ps.setDate(9, Date.valueOf(dpDateOfJoining.getValue()));
            ps.setString(10, cbStatus.getValue());
            ps.setInt(11, editingEmployee.getEmployeeId());
            ps.executeUpdate();
        }
    }

    // ------------------ UTILITY METHODS ------------------

    private boolean validateInput() {
        // Basic check for empty required fields
        if (tfUsername.getText().trim().isEmpty() || tfFirstName.getText().trim().isEmpty() || cbRole.getValue() == null) {
            showAlert("Validation Error", "Please fill in all required user fields.", Alert.AlertType.WARNING);
            return false;
        }
        if (editingEmployee == null && tfPassword.getText().trim().isEmpty()) {
            showAlert("Validation Error", "Password is required for new users.", Alert.AlertType.WARNING);
            return false;
        }
        try {
            // Check if salary is a valid number
            if (!tfSalary.getText().isEmpty()) {
                Double.parseDouble(tfSalary.getText());
            }
        } catch (NumberFormatException e) {
            showAlert("Validation Error", "Salary must be a valid number.", Alert.AlertType.WARNING);
            return false;
        }
        return true;
    }

    @FXML
    private void closeForm() {
        // Gets the current window (Stage) and closes it
        Stage stage = (Stage) lblTitle.getScene().getWindow();
        stage.close();
    }

    private void showAlert(String title, String content, Alert.AlertType type) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}