package controllers;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;
import models.Employee;
import services.EmployeeService;
import java.sql.SQLException;
import java.time.LocalDate;

public class GeneratePayrollModalController {
    @FXML private TextField txtName, txtBaseSalary, txtAllowances, txtDeductions, txtYear;
    @FXML private ComboBox<String> cmbMonth;
    
    private Employee employee;
    private final EmployeeService employeeService = new EmployeeService();

    @FXML
    public void initialize() {
        cmbMonth.setItems(FXCollections.observableArrayList(
            "January", "February", "March", "April", "May", "June", 
            "July", "August", "September", "October", "November", "December"
        ));
        cmbMonth.setValue(LocalDate.now().getMonth().name());
    }

    public void initData(Employee emp) {
        this.employee = emp;
        txtName.setText(emp.getFullName());
        txtBaseSalary.setText(String.format("%.2f", emp.getSalary()));
    }

    @FXML
    private void handleGenerate() {
        try {
            double base = Double.parseDouble(txtBaseSalary.getText());
            double allow = txtAllowances.getText().isEmpty() ? 0 : Double.parseDouble(txtAllowances.getText());
            double deduct = txtDeductions.getText().isEmpty() ? 0 : Double.parseDouble(txtDeductions.getText());
            String month = cmbMonth.getValue();
            int year = Integer.parseInt(txtYear.getText());

            // Simple tax calculation (e.g., 10% of base) or pull from your Settings table
            double tax = base * 0.10; 

            employeeService.savePayroll(employee.getEmployeeId(), month, year, base, allow, deduct, tax);
            
            Alert alert = new Alert(Alert.AlertType.INFORMATION, "Payroll record created successfully!");
            alert.showAndWait();
            handleCancel();
        } catch (Exception e) {
            new Alert(Alert.AlertType.ERROR, "Invalid input: " + e.getMessage()).show();
        }
    }

    @FXML private void handleCancel() {
        ((Stage) txtName.getScene().getWindow()).close();
    }
}