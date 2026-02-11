package controllers;

import java.net.URL;
import java.sql.*;
import java.time.LocalDate;
import java.util.ResourceBundle;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.Alert.AlertType;
import javafx.stage.Stage;
import models.Employee;
import utils.DBConnection; 

public class EditEmployeeController implements Initializable {

    @FXML private TextField txtEmployeeId, txtUsername, txtFirstName, txtLastName, txtEmail, txtPhone, txtDesignation, txtSalary, txtBankAccount;
    @FXML private PasswordField txtNewPassword; 
    @FXML private ComboBox<String> cbRole, cbDepartment, cbStatus;
    @FXML private DatePicker dpDateJoined;
    @FXML private ToggleGroup tgGender;
    @FXML private RadioButton rbMale, rbFemale, rbOther;

    private Employee employeeToEdit;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        cbStatus.setItems(FXCollections.observableArrayList("Active", "Inactive"));
        loadComboBoxData();
    }
    
    public void setEmployeeToEdit(Employee emp) {
        if (emp == null) return;
        this.employeeToEdit = emp;
        
        txtEmployeeId.setText(String.valueOf(emp.getEmployeeId()));
        txtUsername.setText(emp.getUsername());
        txtFirstName.setText(emp.getFirstName());
        txtLastName.setText(emp.getLastName());
        txtEmail.setText(emp.getEmail());
        txtPhone.setText(emp.getPhone());
        txtDesignation.setText(emp.getPosition()); 
        txtSalary.setText(String.valueOf(emp.getSalary()));
        txtBankAccount.setText(emp.getBankAccount()); 
        dpDateJoined.setValue(emp.getDateJoined());
       
        cbRole.getSelectionModel().select(emp.getRoleName());
        cbDepartment.getSelectionModel().select(emp.getDepartment()); 
        cbStatus.getSelectionModel().select(emp.getStatus());
        
        if (emp.getGender() != null) {
            String g = emp.getGender().toLowerCase();
            if (g.equals("male")) rbMale.setSelected(true);
            else if (g.equals("female")) rbFemale.setSelected(true);
            else rbOther.setSelected(true);
        }
    }

    @FXML
    private void handleUpdateEmployee() {
        if (employeeToEdit == null) {
            showAlert(AlertType.ERROR, "Error", "No employee data loaded.");
            return;
        }
        
        try {
            if (!validateRequiredFields() || !validateUniqueFields()) return; 
            executeDatabaseUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
            showAlert(AlertType.ERROR, "Database Error", "Update failed: " + e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            showAlert(AlertType.ERROR, "Error", "A system error occurred: " + e.getMessage());
        }
    }

    private void executeDatabaseUpdate() throws SQLException {
        // 1. Get and Validate Foreign Keys
        int roleId = getRoleId(cbRole.getValue());
        int deptId = getDeptId(cbDepartment.getValue());

        if (roleId == 0) throw new SQLException("Role '" + cbRole.getValue() + "' not found in database.");
        if (deptId == 0) throw new SQLException("Department '" + cbDepartment.getValue() + "' not found in database.");

        Connection conn = null;
        try {
            conn = DBConnection.getConnection();
            conn.setAutoCommit(false); 

            // 2. UPDATE USERS TABLE
            String userSql = "UPDATE users SET username=?, first_name=?, last_name=?, email=?, phone=?, designation=?, date_of_joining=?, status=?, role_id=?, dept_id=? WHERE user_id=?";
            try (PreparedStatement pst = conn.prepareStatement(userSql)) {
                pst.setString(1, txtUsername.getText().trim());
                pst.setString(2, txtFirstName.getText().trim());
                pst.setString(3, txtLastName.getText().trim());
                pst.setString(4, txtEmail.getText().trim());
                pst.setString(5, txtPhone.getText().trim());
                pst.setString(6, txtDesignation.getText().trim());
                pst.setDate(7, Date.valueOf(dpDateJoined.getValue()));
                pst.setString(8, cbStatus.getValue());
                pst.setInt(9, roleId);
                pst.setInt(10, deptId); 
                pst.setInt(11, employeeToEdit.getUserId());
                pst.executeUpdate();
            }

            // 3. UPDATE EMPLOYEES TABLE
            String empSql = "UPDATE employees SET gender=?, position=?, salary=?, bank_account=?, status=? WHERE emp_id=?";
            try (PreparedStatement pst = conn.prepareStatement(empSql)) {
                RadioButton selectedGender = (RadioButton) tgGender.getSelectedToggle();
                pst.setString(1, selectedGender.getText());
                pst.setString(2, txtDesignation.getText().trim());
                pst.setDouble(3, Double.parseDouble(txtSalary.getText().trim()));
                pst.setString(4, txtBankAccount.getText().trim());
                pst.setString(5, cbStatus.getValue());
                pst.setInt(6, employeeToEdit.getEmployeeId());
                pst.executeUpdate();
            }

            // 4. OPTIONAL: PASSWORD UPDATE
            if (!txtNewPassword.getText().isEmpty()) {
                String passSql = "UPDATE users SET password=? WHERE user_id=?";
                try (PreparedStatement pst = conn.prepareStatement(passSql)) {
                    pst.setString(1, txtNewPassword.getText().trim()); // Suggestion: Use hashing here
                    pst.setInt(2, employeeToEdit.getUserId());
                    pst.executeUpdate();
                }
            }
            
            conn.commit();
            showAlert(AlertType.INFORMATION, "Success", "Employee record updated successfully.");
            handleCancel(null);

        } catch (SQLException e) {
            if (conn != null) conn.rollback();
            throw e;
        } finally {
            if (conn != null) conn.close();
        }
    }

    private int getRoleId(String name) throws SQLException {
        if (name == null) return 0;
        String sql = "SELECT role_id FROM roles WHERE UPPER(role_name) = UPPER(?)";
        try (Connection conn = DBConnection.getConnection(); PreparedStatement pst = conn.prepareStatement(sql)) {
            pst.setString(1, name.trim());
            ResultSet rs = pst.executeQuery();
            return rs.next() ? rs.getInt(1) : 0;
        }
    }

    private int getDeptId(String name) throws SQLException {
        if (name == null) return 0;
        String sql = "SELECT dept_id FROM departments WHERE UPPER(dept_name) = UPPER(?)";
        try (Connection conn = DBConnection.getConnection(); PreparedStatement pst = conn.prepareStatement(sql)) {
            pst.setString(1, name.trim());
            ResultSet rs = pst.executeQuery();
            return rs.next() ? rs.getInt(1) : 0;
        }
    }

    private boolean validateRequiredFields() {
        if (txtUsername.getText().isEmpty() || txtFirstName.getText().isEmpty() || 
            txtEmail.getText().isEmpty() || txtSalary.getText().isEmpty() || 
            cbRole.getValue() == null || cbDepartment.getValue() == null ||
            dpDateJoined.getValue() == null || tgGender.getSelectedToggle() == null) {
            showAlert(AlertType.WARNING, "Incomplete Form", "Please fill in all required fields.");
            return false;
        }
        return true;
    }

    private boolean validateUniqueFields() throws SQLException {
        String newUsername = txtUsername.getText().trim();
        String newEmail = txtEmail.getText().trim();
        
        if (!newUsername.equalsIgnoreCase(employeeToEdit.getUsername()) && isExists("username", newUsername)) {
            showAlert(AlertType.WARNING, "Conflict", "Username already exists.");
            return false;
        }
        if (!newEmail.equalsIgnoreCase(employeeToEdit.getEmail()) && isExists("email", newEmail)) {
            showAlert(AlertType.WARNING, "Conflict", "Email already exists.");
            return false;
        }
        return true;
    }

    private boolean isExists(String col, String val) throws SQLException {
        String sql = "SELECT COUNT(*) FROM users WHERE " + col + " = ? AND user_id != ?";
        try (Connection conn = DBConnection.getConnection(); PreparedStatement pst = conn.prepareStatement(sql)) {
            pst.setString(1, val);
            pst.setInt(2, employeeToEdit.getUserId());
            ResultSet rs = pst.executeQuery();
            return rs.next() && rs.getInt(1) > 0;
        }
    }

    private void loadComboBoxData() {
        try (Connection conn = DBConnection.getConnection()) {
            ResultSet rsR = conn.createStatement().executeQuery("SELECT role_name FROM roles");
            while (rsR.next()) cbRole.getItems().add(rsR.getString("role_name"));
            
            ResultSet rsD = conn.createStatement().executeQuery("SELECT dept_name FROM departments");
            while (rsD.next()) cbDepartment.getItems().add(rsD.getString("dept_name"));
        } catch (SQLException e) { e.printStackTrace(); }
    }

    @FXML private void handleCancel(ActionEvent event) {
        ((Stage) txtUsername.getScene().getWindow()).close();
    }

    private void showAlert(AlertType type, String title, String msg) {
        Alert a = new Alert(type); a.setTitle(title); a.setHeaderText(null); a.setContentText(msg); a.showAndWait();
    }
}