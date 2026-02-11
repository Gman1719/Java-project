package controllers;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.event.ActionEvent;
import java.time.LocalDate;
import java.sql.SQLException;
import java.util.List;
import java.util.regex.Pattern;

import services.UserService;
import javafx.collections.FXCollections;

public class HRAddEmployeeController {

    // FXML fields from the updated FXML
    @FXML private TextField txtFirstName;
    @FXML private TextField txtLastName;
    @FXML private TextField txtEmail;
    @FXML private ComboBox<String> cmbGender;
    @FXML private ComboBox<String> cmbDepartment;
    @FXML private TextField txtJobTitle;
    @FXML private DatePicker dpHireDate;
    @FXML private TextField txtSalary;

    private UserService userService = new UserService();
    
    // Standard basic email validation
    private static final Pattern EMAIL_PATTERN = 
        Pattern.compile("^[\\w-\\.]+@([\\w-]+\\.)+[\\w-]{2,4}$");

    @FXML
    public void initialize() {
        // 1. Load departments from the database
        loadDepartments();
        
        // 2. Set default Hire Date
        dpHireDate.setValue(LocalDate.now());

        // 3. Force salary field to only accept numbers (optional, but good practice)
        txtSalary.textProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null && !newValue.matches("\\d*(\\.\\d{0,2})?")) {
                txtSalary.setText(oldValue);
            }
        });
    }

    private void loadDepartments() {
        try {
            // NOTE: Assumes you have added getAllDepartmentNames() to UserService
            List<String> deptNames = userService.getAllDepartmentNames(); 
            cmbDepartment.setItems(FXCollections.observableArrayList(deptNames));
        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Database Error", "Failed to load departments. Check database connection and 'departments' table.");
            e.printStackTrace();
        }
    }

    @FXML
    private void handleAddEmployee(ActionEvent event) {
        String firstName = txtFirstName.getText().trim();
        String lastName = txtLastName.getText().trim();
        String email = txtEmail.getText().trim();
        String gender = cmbGender.getValue();
        String departmentName = cmbDepartment.getValue();
        String jobTitle = txtJobTitle.getText().trim();
        LocalDate hireDate = dpHireDate.getValue();
        String salaryStr = txtSalary.getText().trim();
        double salary;
        
        // --- 1. Validation Logic ---
        String validationError = validateFields(firstName, lastName, email, gender, departmentName, jobTitle, hireDate, salaryStr);
        if (validationError != null) {
            showAlert(Alert.AlertType.ERROR, "Validation Error", validationError);
            return;
        }

        try {
            salary = Double.parseDouble(salaryStr);
        } catch (NumberFormatException e) {
            showAlert(Alert.AlertType.ERROR, "Validation Error", "Invalid salary amount.");
            return;
        }

        // --- 2. Database Creation Logic ---
        try {
            // Lookup IDs from names
            int deptId = userService.getDepartmentId(departmentName);
            // We default new users to the 'Employee' role (ID 3 in your schema)
            int employeeRoleId = userService.getRoleId("Employee"); 
            
            if (deptId <= 0 || employeeRoleId <= 0) {
                showAlert(Alert.AlertType.ERROR, "Data Lookup Error", "Failed to find valid IDs for Department or Role. Check 'roles' and 'departments' tables.");
                return;
            }

            // Execute the two-part transaction (users and employees inserts)
            int newUserId = userService.createNewEmployee(
                firstName, lastName, email, gender, deptId, jobTitle, hireDate, salary, employeeRoleId);

            if (newUserId > 0) {
                String defaultUsername = (firstName.substring(0, 1) + lastName).toLowerCase();
                showAlert(Alert.AlertType.INFORMATION, "Success", 
                          "New employee " + firstName + " " + lastName + " created (User ID: " + newUserId + ").\n" +
                          "Default Username: " + defaultUsername + 
                          "\nDefault Password: password123 (MUST be changed)");
                
                clearForm();
            } else {
                showAlert(Alert.AlertType.ERROR, "Creation Failed", "Employee record could not be created in the database.");
            }

        } catch (SQLException e) {
            // Check for unique constraint violation (username/email already exists)
            if (e.getMessage() != null && e.getMessage().contains("Duplicate entry")) {
                 showAlert(Alert.AlertType.ERROR, "Database Error", "User with that generated username or email likely already exists. Try a different name.");
            } else {
                 showAlert(Alert.AlertType.ERROR, "Database Error", "An error occurred during employee creation. Check console for details.");
            }
            e.printStackTrace();
        }
    }
    
    // --- Validation Helper Method ---
    private String validateFields(String firstName, String lastName, String email, String gender, 
                                  String departmentName, String jobTitle, LocalDate hireDate, String salaryStr) {
        if (firstName.isEmpty() || lastName.isEmpty() || email.isEmpty() || gender == null || 
            departmentName == null || jobTitle.isEmpty() || hireDate == null || salaryStr.isEmpty()) {
            return "All fields must be completed.";
        }
        if (!EMAIL_PATTERN.matcher(email).matches()) {
            return "Please enter a valid email address.";
        }
        if (hireDate.isAfter(LocalDate.now())) {
             return "Hire Date cannot be in the future.";
        }
        if (!salaryStr.matches("\\d+(\\.\\d{1,2})?")) {
            return "Salary must be a valid number (e.g., 25000.00).";
        }
        return null;
    }

    private void clearForm() {
        txtFirstName.clear();
        txtLastName.clear();
        txtEmail.clear();
        cmbGender.getSelectionModel().clearSelection();
        cmbDepartment.getSelectionModel().clearSelection();
        txtJobTitle.clear();
        dpHireDate.setValue(LocalDate.now());
        txtSalary.clear();
    }

    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}