package controllers;

import javafx.fxml.FXML;
import javafx.fxml.Initializable; // Import the Initializable interface
import javafx.scene.control.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import utils.DBConnection;

import java.net.URL; // Needed for Initializable
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ResourceBundle; // Needed for Initializable

// ⭐ FIX: Missing import for the Employee model
import models.Employee; // Assuming the model is in the 'models' package

public class PayrollController implements Initializable { // Implement Initializable

    @FXML private ComboBox<Employee> employeeCombo;
    @FXML private DatePicker payrollMonth;
    @FXML private TextField txtBasicSalary;
    @FXML private TextField txtAllowance;
    @FXML private TextField txtDeductions;
    @FXML private Label lblNetSalary;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // --- PRIMARY FIX: Handle SQLException here ---
        try {
            loadEmployees();
        } catch (SQLException e) {
            e.printStackTrace();
            showAlert("Database Error", "Failed to connect or load employee list on startup: " + e.getMessage(), Alert.AlertType.ERROR);
            employeeCombo.setDisable(true); // Disable input if data load fails
            return;
        }
        
        // --- ComboBox Configuration ---
        employeeCombo.setCellFactory(lv -> new ListCell<Employee>() {
            @Override
            protected void updateItem(Employee item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty ? null : item != null ? item.getFullName() : null);
            }
        });
        
        employeeCombo.setButtonCell(new ListCell<Employee>() {
            @Override
            protected void updateItem(Employee item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty ? "Select Employee" : item != null ? item.getFullName() : "Select Employee");
            }
        });

        // --- Listener Configuration ---
        // Set up listeners for input fields to trigger recalculation immediately
        txtBasicSalary.textProperty().addListener((obs, oldVal, newVal) -> calculateNetSalary());
        txtAllowance.textProperty().addListener((obs, oldVal, newVal) -> calculateNetSalary());
        txtDeductions.textProperty().addListener((obs, oldVal, newVal) -> calculateNetSalary());


        // Listener to populate salary fields when an employee is selected
        employeeCombo.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                // Ensure only two decimal places are displayed
                txtBasicSalary.setText(String.format("%.2f", newVal.getSalary())); 
                calculateNetSalary(); 
            } else {
                txtBasicSalary.clear();
                txtAllowance.clear();
                txtDeductions.clear();
                lblNetSalary.setText("Net Salary: 0.00");
            }
        });
        
        // Initialize calculation once at the end
        calculateNetSalary();
    }
    

    private void loadEmployees() throws SQLException { 
        ObservableList<Employee> employees = FXCollections.observableArrayList();
        
        // Query to get required data from joining employee and user tables
        String sql = "SELECT e.emp_id, u.user_id, u.first_name, u.last_name, u.username, e.salary " +
                     "FROM employees e " + 
                     "JOIN users u ON e.user_id = u.user_id";

        // Use try-with-resources to ensure connection is closed
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            
            // ⭐ IMPROVEMENT: Check conn != null first (handled by DBConnection throwing exception in a robust setup)
            if (conn == null || conn.isClosed()) {
                throw new SQLException("Database connection is not available.");
            }

            while (rs.next()) {
                int id = rs.getInt("emp_id");
                int userId = rs.getInt("user_id"); 
                String firstName = rs.getString("first_name");
                String lastName = rs.getString("last_name");
                String username = rs.getString("username");
                double salary = rs.getDouble("salary");
                
                employees.add(new Employee(id, userId, username, firstName, lastName, salary));
            }
            employeeCombo.setItems(employees);

        }
    }

    // ------------------ CALCULATIONS ------------------

    @FXML 
    private void onCalculateNetSalary() {
        calculateNetSalary();
    }
    
    private void calculateNetSalary() {
        try {
            // Use replaceAll to strip any characters that aren't digits or decimal point
            double basic = txtBasicSalary.getText().isEmpty() ? 0 : Double.parseDouble(txtBasicSalary.getText().replaceAll("[^\\d\\.]", ""));
            double allowance = txtAllowance.getText().isEmpty() ? 0 : Double.parseDouble(txtAllowance.getText().replaceAll("[^\\d\\.]", ""));
            double deductions = txtDeductions.getText().isEmpty() ? 0 : Double.parseDouble(txtDeductions.getText().replaceAll("[^\\d\\.]", ""));

            // Placeholder calculation
            double netSalary = basic + allowance - deductions;

            lblNetSalary.setText(String.format("Net Salary: $%.2f", netSalary));
        } catch (NumberFormatException e) {
            lblNetSalary.setText("Net Salary: Invalid Input");
        }
    }

    // ------------------ GENERATION ------------------

    @FXML
    private void onGeneratePayroll() {
        Employee selectedEmployee = employeeCombo.getSelectionModel().getSelectedItem();
        LocalDate selectedMonth = payrollMonth.getValue();

        if (selectedEmployee == null || selectedMonth == null) {
            showAlert("Missing Data", "Please select an employee and a payroll month.", Alert.AlertType.WARNING);
            return;
        }

        try {
            // Re-validate inputs
            double basic = Double.parseDouble(txtBasicSalary.getText().replaceAll("[^\\d\\.]", ""));
            double allowance = txtAllowance.getText().isEmpty() ? 0 : Double.parseDouble(txtAllowance.getText().replaceAll("[^\\d\\.]", ""));
            double deductions = txtDeductions.getText().isEmpty() ? 0 : Double.parseDouble(txtDeductions.getText().replaceAll("[^\\d\\.]", ""));
            
            // Get month name and year
            String monthName = selectedMonth.getMonth().toString();
            int year = selectedMonth.getYear();

            // SQL statement for payroll insertion
            String sql = "INSERT INTO payroll (emp_id, month, year, base_salary, allowances, deductions) " +
                         "VALUES (?, ?, ?, ?, ?, ?)";
            
            try (Connection conn = DBConnection.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                
                ps.setInt(1, selectedEmployee.getEmployeeId()); 
                ps.setString(2, monthName);
                ps.setInt(3, year);
                ps.setDouble(4, basic);
                ps.setDouble(5, allowance);
                ps.setDouble(6, deductions);
                
                int rowsAffected = ps.executeUpdate();
                if (rowsAffected > 0) {
                    showAlert("Success", "Payroll generated successfully for " + selectedEmployee.getFullName() + " for " + monthName + " " + year + ".", Alert.AlertType.INFORMATION);
                } else {
                    showAlert("Failed", "Failed to insert payroll record. Check for duplicate month/year entry.", Alert.AlertType.ERROR);
                }
            }

        } catch (NumberFormatException e) {
             showAlert("Input Error", "Please ensure all salary fields contain valid numbers.", Alert.AlertType.WARNING);
        } catch (SQLException e) {
            e.printStackTrace();
            showAlert("Database Error", "Failed to generate payroll: " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    private void showAlert(String title, String content, Alert.AlertType type) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}