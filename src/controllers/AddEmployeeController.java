package controllers;

import java.net.URL;
import java.sql.*;
import java.time.LocalDate;
import java.util.List;
import java.util.ResourceBundle;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.Alert.AlertType;
import javafx.stage.Stage;
import utils.DBConnection; 
// Assuming you will implement a utility for password hashing, e.g., using BCrypt
// import utils.SecurityUtility; 

public class AddEmployeeController implements Initializable {

    // --- FXML Fields for User Data (Left Side) ---
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
    @FXML private ComboBox<String> cbStatus;

    // --- FXML Fields for Employee Data (Right Side) ---
    @FXML private ToggleGroup tgGender;
    @FXML private TextField txtSalary;
    @FXML private TextField txtBankAccount;

    // --- Regex Patterns for Validation ---
    private static final String EMAIL_PATTERN = 
        "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,6}$";
    
    // Pattern for Ethiopian phone numbers: Must be 10 digits total.
    // Starts with 09 or +2519, followed by 8 more digits.
    private static final String PHONE_PATTERN = 
        "^(09|\\+2519)\\d{8}$";

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        // Set default values and load dynamic data
        cbStatus.setItems(FXCollections.observableArrayList("Active", "Inactive"));
        cbStatus.getSelectionModel().select("Active");
        dpDateJoined.setValue(LocalDate.now());

        loadComboBoxData();
    }

    // --- Helper Methods (Unchanged, assuming DBConnection is fine) ---
    // (getAllRoleNames, getAllDepartmentNames, getRoleId, getDeptId, getSelectedGender remain the same)
    
    // DAO method: Get list of all role names
    private List<String> getAllRoleNames() throws SQLException {
        List<String> roleNames = new java.util.ArrayList<>();
        String sql = "SELECT role_name FROM roles";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pst = conn.prepareStatement(sql);
             ResultSet rs = pst.executeQuery()) {
            while (rs.next()) {
                roleNames.add(rs.getString("role_name"));
            }
        }
        return roleNames;
    }
    
    // DAO method: Get list of all department names
    private List<String> getAllDepartmentNames() throws SQLException {
        List<String> deptNames = new java.util.ArrayList<>();
        String sql = "SELECT dept_name FROM departments";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pst = conn.prepareStatement(sql);
             ResultSet rs = pst.executeQuery()) {
            while (rs.next()) {
                deptNames.add(rs.getString("dept_name"));
            }
        }
        return deptNames;
    }

    private void loadComboBoxData() {
        try {
            cbRole.setItems(FXCollections.observableArrayList(getAllRoleNames()));
            cbDepartment.setItems(FXCollections.observableArrayList(getAllDepartmentNames()));
        } catch (SQLException e) {
            showAlert(AlertType.ERROR, "Database Error", "Failed to load roles or departments: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private int getRoleId(String roleName) throws SQLException {
        String sql = "SELECT role_id FROM roles WHERE role_name = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pst = conn.prepareStatement(sql)) {
            pst.setString(1, roleName);
            try (ResultSet rs = pst.executeQuery()) {
                if (rs.next()) return rs.getInt("role_id");
            }
        }
        throw new SQLException("Role not found: " + roleName);
    }
    
    private int getDeptId(String deptName) throws SQLException {
        String sql = "SELECT dept_id FROM departments WHERE dept_name = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pst = conn.prepareStatement(sql)) {
            pst.setString(1, deptName);
            try (ResultSet rs = pst.executeQuery()) {
                if (rs.next()) return rs.getInt("dept_id");
            }
        }
        throw new SQLException("Department not found: " + deptName);
    }
    
    private String getSelectedGender() {
        if (tgGender.getSelectedToggle() == null) return null;
        return ((RadioButton) tgGender.getSelectedToggle()).getText();
    }
    
    // --- Core Transaction Handler (Updated for Password Hashing Reminder) ---

    @FXML
    private void handleSaveEmployee(ActionEvent event) {
        Connection conn = null;
        int generatedUserId = -1;

        // 0. Pre-validation and data gathering (uses the new robust validation)
        if (!validateRequiredFields()) return;
        
        try {
            int roleId = getRoleId(cbRole.getValue());
            int deptId = getDeptId(cbDepartment.getValue());
            String gender = getSelectedGender();
            double salary = Double.parseDouble(txtSalary.getText().trim());
            
            // ⭐ SECURITY UPDATE: HASH THE PASSWORD HERE
            // String hashedPassword = SecurityUtility.hashPassword(txtPassword.getText().trim());
            String rawPassword = txtPassword.getText().trim(); // Using raw for demonstration, MUST be hashed.

            // 1. Establish connection and set up transaction
            conn = DBConnection.getConnection();
            conn.setAutoCommit(false); // Start Transaction

            // ===============================================
            // ⭐ STEP 1: INSERT INTO USERS TABLE
            // ===============================================
            String userSql = "INSERT INTO users (username, password, first_name, last_name, email, phone, dept_id, designation, date_of_joining, status, role_id) " +
                             "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
            
            try (PreparedStatement userPst = conn.prepareStatement(userSql, Statement.RETURN_GENERATED_KEYS)) {
                
                // Bind User Parameters (1-11)
                userPst.setString(1, txtUsername.getText().trim());
                userPst.setString(2, rawPassword); // Replace with HASHED password!
                userPst.setString(3, txtFirstName.getText().trim());
                userPst.setString(4, txtLastName.getText().trim());
                userPst.setString(5, txtEmail.getText().trim());
                userPst.setString(6, txtPhone.getText().trim());
                userPst.setInt(7, deptId);
                userPst.setString(8, txtDesignation.getText().trim());
                userPst.setDate(9, Date.valueOf(dpDateJoined.getValue()));
                userPst.setString(10, cbStatus.getValue());
                userPst.setInt(11, roleId);
                
                userPst.executeUpdate();

                // ... (Steps 2 and 3 remain the same, using generatedUserId) ...
                try (ResultSet rs = userPst.getGeneratedKeys()) {
                    if (rs.next()) {
                        generatedUserId = rs.getInt(1); 
                    } else {
                        throw new SQLException("User record created but failed to retrieve user ID.");
                    }
                }
            }

            // ===============================================
            // ⭐ STEP 3: INSERT INTO EMPLOYEES TABLE
            // ===============================================
            String empSql = "INSERT INTO employees (user_id, role_id, gender, phone, email, dept_id, position, salary, bank_account, date_joined, status) " +
                            "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
            
            try (PreparedStatement empPst = conn.prepareStatement(empSql)) {
                
                empPst.setInt(1, generatedUserId); // KEY LINK!
                empPst.setInt(2, roleId);
                empPst.setString(3, gender);
                empPst.setString(4, txtPhone.getText().trim());
                empPst.setString(5, txtEmail.getText().trim());
                empPst.setInt(6, deptId);
                empPst.setString(7, txtDesignation.getText().trim());
                empPst.setDouble(8, salary);
                empPst.setString(9, txtBankAccount.getText().trim());
                empPst.setDate(10, Date.valueOf(dpDateJoined.getValue()));
                empPst.setString(11, cbStatus.getValue());
                
                empPst.executeUpdate();
            }
            
            // Finalize the transaction
            conn.commit(); 
            showAlert(AlertType.INFORMATION, "Success", "Employee and User records created with User ID: " + generatedUserId);
            handleReset(event);

        } catch (SQLException e) {
            // ... (Error handling remains the same) ...
            if (conn != null) {
                try {
                    conn.rollback(); 
                } catch (SQLException rollbackEx) {
                    rollbackEx.printStackTrace();
                }
            }
            if (e.getMessage().contains("Duplicate entry") && e.getMessage().contains("username")) {
                showAlert(AlertType.ERROR, "Database Error", "The username '" + txtUsername.getText() + "' already exists.");
            } else {
                e.printStackTrace();
                showAlert(AlertType.ERROR, "Database Error", "Transaction failed. Check logs. Reason: " + e.getMessage());
            }
        } catch (NumberFormatException e) {
             // This catch is now redundant but kept for safety, as validation handles it.
            showAlert(AlertType.WARNING, "Input Error", "Please ensure Salary is a valid numeric value (e.g., 50000.00).");
        } catch (Exception e) {
             showAlert(AlertType.ERROR, "Error", "A general error occurred: " + e.getMessage());
        } finally {
            if (conn != null) {
                try {
                    conn.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        }
    }
    
    // --- VALIDATION METHOD (Robust Line-by-Line Checks) ---

    private boolean validateRequiredFields() {
        // Use a StringBuilder to collect all validation errors
        StringBuilder errors = new StringBuilder();

        // 1. Text Field and Null Checks
        checkTextField(txtUsername, "Username", errors);
        checkTextField(txtPassword, "Password", errors);
        
        // 2. Name Validation (First Name)
        if (!checkTextField(txtFirstName, "First Name", errors) && !isValidName(txtFirstName.getText())) {
            errors.append("- First Name cannot be purely numeric or contain special characters.\n");
        }
        
        // 3. Name Validation (Last Name)
        if (!checkTextField(txtLastName, "Last Name", errors) && !isValidName(txtLastName.getText())) {
            errors.append("- Last Name cannot be purely numeric or contain special characters.\n");
        }
        
        // 4. Email Validation
        if (checkTextField(txtEmail, "Email", errors)) { // Check if field is empty first
             if (!isValidEmail(txtEmail.getText())) {
                errors.append("- Email format is invalid (e.g., user@example.com).\n");
            }
        }
        
        // 5. Phone Validation
        if (checkTextField(txtPhone, "Phone Number", errors)) {
            if (!isValidPhone(txtPhone.getText())) {
                errors.append("- Phone Number must start with '09' or '+2519' and have exactly 10 digits total.\n");
            }
        }

        // 6. ComboBox and Toggle Checks
        if (cbRole.getValue() == null) errors.append("- Role is required.\n");
        if (cbDepartment.getValue() == null) errors.append("- Department is required.\n");
        checkTextField(txtDesignation, "Designation", errors);
        checkTextField(txtBankAccount, "Bank Account", errors);
        
        if (tgGender.getSelectedToggle() == null) errors.append("- Gender is required.\n");

        // 7. Date Validation (Date Joined)
        if (dpDateJoined.getValue() == null) {
            errors.append("- Date Joined is required.\n");
        } else if (dpDateJoined.getValue().isAfter(LocalDate.now())) {
            errors.append("- Date Joined cannot be in the future.\n");
        }

        // 8. Salary Validation
        if (checkTextField(txtSalary, "Base Salary", errors)) {
            if (!isValidSalary(txtSalary.getText())) {
                 errors.append("- Base Salary must be a positive numeric value.\n");
            }
        }
        
        // Final Check
        if (errors.length() > 0) {
            showAlert(AlertType.WARNING, "Validation Errors", "Please correct the following issues:\n" + errors.toString());
            return false;
        }
        return true;
    }

    // --- Specific Validation Helper Methods ---

    /** Checks if a TextField is null or empty. Returns true if valid/filled. */
    private boolean checkTextField(TextField field, String fieldName, StringBuilder errors) {
        if (field.getText().trim().isEmpty()) {
            errors.append("- ").append(fieldName).append(" is required.\n");
            return false;
        }
        return true;
    }
    
    /** Checks for basic name structure (prevents purely numeric/special chars). */
    private boolean isValidName(String name) {
        // Allows letters, spaces, hyphens, and apostrophes. Prevents empty or only numbers/symbols.
        return name.matches("[a-zA-Z\\s\\-']+") && !name.trim().isEmpty();
    }

    /** Checks if the email matches the basic pattern. */
    private boolean isValidEmail(String email) {
        Pattern pattern = Pattern.compile(EMAIL_PATTERN);
        Matcher matcher = pattern.matcher(email);
        return matcher.matches();
    }

    /** Checks if salary is a positive number. */
    private boolean isValidSalary(String salaryStr) {
        try {
            double salary = Double.parseDouble(salaryStr.trim());
            return salary > 0;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    /** Checks if the phone number matches the Ethiopian pattern. */
    private boolean isValidPhone(String phone) {
        Pattern pattern = Pattern.compile(PHONE_PATTERN);
        Matcher matcher = pattern.matcher(phone.trim());
        return matcher.matches();
    }
    
    // --- Other Utility Methods (Unchanged) ---
    
    @FXML
    private void handleReset(ActionEvent event) {
        txtUsername.clear();
        txtPassword.clear();
        txtFirstName.clear();
        txtLastName.clear();
        txtEmail.clear();
        txtPhone.clear();
        cbRole.getSelectionModel().clearSelection();
        cbDepartment.getSelectionModel().clearSelection();
        txtDesignation.clear();
        dpDateJoined.setValue(LocalDate.now()); 
        cbStatus.getSelectionModel().select("Active");
        
        if (tgGender.getSelectedToggle() != null) tgGender.getSelectedToggle().setSelected(false);
        txtSalary.clear();
        txtBankAccount.clear();
    }
    
    @FXML
    private void handleCancel(ActionEvent event) {
        Stage stage = (Stage) ((javafx.scene.control.Button) event.getSource()).getScene().getWindow();
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