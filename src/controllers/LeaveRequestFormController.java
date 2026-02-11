package controllers;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.stage.Stage;
import java.net.URL;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ResourceBundle;
import javafx.scene.control.Alert.AlertType;
import utils.DBConnection; 
// You might also need utils.SessionManager if you wanted to load the ID here, 
// but it's better to pass it from the main dashboard controller.

public class LeaveRequestFormController implements Initializable {

    // --- FXML Elements ---
    @FXML private Label lblAvailableDays;
    @FXML private ComboBox<String> cbLeaveType;
    @FXML private DatePicker dpStartDate;
    @FXML private DatePicker dpEndDate;
    @FXML private Label lblTotalDays;
    @FXML private TextArea txtReason;
    
    // --- Internal State ---
    // employeeId must be set by the calling controller (EmployeeDashboardController)
    private int employeeId; 
    private final double ANNUAL_LEAVE_BALANCE = 15.0; 
    private final double SICK_LEAVE_BALANCE = 10.0;
    private double currentAvailableDays = ANNUAL_LEAVE_BALANCE; 

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        cbLeaveType.getItems().addAll("Annual Leave", "Sick Leave", "Maternity/Paternity", "Unpaid Leave");
        
        cbLeaveType.valueProperty().addListener((obs, oldType, newType) -> updateAvailableDays(newType));
        
        dpStartDate.valueProperty().addListener((obs, oldDate, newDate) -> calculateDays());
        dpEndDate.valueProperty().addListener((obs, oldDate, newDate) -> calculateDays());

        lblAvailableDays.setText("Available Annual Leave Days: " + ANNUAL_LEAVE_BALANCE);
        cbLeaveType.setValue("Annual Leave"); 
    }
    
    /**
     * Called by the parent controller (EmployeeDashboardController) to set the context.
     * THIS MUST BE CALLED OR employeeId WILL BE 0.
     */
    public void setEmployeeId(int employeeId) {
        this.employeeId = employeeId;
        // Optional: If you load balances from DB, this is where you'd call a method to load them.
    }
    
    private void updateAvailableDays(String leaveType) {
        if (leaveType == null) return;
        
        switch (leaveType) {
            case "Annual Leave":
                currentAvailableDays = ANNUAL_LEAVE_BALANCE;
                lblAvailableDays.setText("Available Annual Leave Days: " + currentAvailableDays);
                break;
            case "Sick Leave":
                currentAvailableDays = SICK_LEAVE_BALANCE;
                lblAvailableDays.setText("Available Sick Leave Days: " + currentAvailableDays);
                break;
            case "Unpaid Leave":
                currentAvailableDays = Double.MAX_VALUE;
                lblAvailableDays.setText("Unpaid Leave: No balance check required.");
                break;
            default:
                currentAvailableDays = 0;
                lblAvailableDays.setText("Please select a Leave Type.");
                break;
        }
        calculateDays(); 
    }
    
    private void calculateDays() {
        LocalDate start = dpStartDate.getValue();
        LocalDate end = dpEndDate.getValue();
        
        if (start != null && end != null && !end.isBefore(start)) {
            // Include start and end date (+1)
            long days = ChronoUnit.DAYS.between(start, end) + 1; 
            lblTotalDays.setText(days + " Days");
        } else {
            lblTotalDays.setText("0 Days");
        }
    }

    @FXML
    private void handleSubmitRequest() {
        if (!validateInput()) return;
        
        String type = cbLeaveType.getValue();
        LocalDate start = dpStartDate.getValue();
        LocalDate end = dpEndDate.getValue();
        String reason = txtReason.getText().trim();
        
        double totalDays = 0.0;
        try {
            totalDays = Double.parseDouble(lblTotalDays.getText().split(" ")[0]);
        } catch (NumberFormatException e) {
            showAlert(AlertType.ERROR, "Error", "Invalid total days calculated.");
            return;
        }

        // 1. Balance Check
        if ((type.equals("Annual Leave") || type.equals("Sick Leave")) && totalDays > currentAvailableDays) {
            showAlert(AlertType.WARNING, "Cannot Submit", String.format("Requested days (%.1f) exceed available balance (%.1f).", totalDays, currentAvailableDays));
            return;
        }
        
        // 2. Employee ID Check (THE PROBLEM IS UPSTREAM, BUT THIS CHECK IS GOOD)
        if (employeeId <= 0) {
            showAlert(AlertType.ERROR, "Error", "Employee ID not set. Cannot submit request. Please ensure you are logged in correctly.");
            return;
        }

        // 3. Database Insertion
        String sql = "INSERT INTO leave_requests (emp_id, leave_type, start_date, end_date, total_days, reason, status) " +
                     "VALUES (?, ?, ?, ?, ?, ?, 'Pending')";
        
        try (Connection conn = getDBConnection(); 
             PreparedStatement pst = conn.prepareStatement(sql)) {
            
            pst.setInt(1, employeeId);
            pst.setString(2, type);
            pst.setDate(3, java.sql.Date.valueOf(start));
            pst.setDate(4, java.sql.Date.valueOf(end));
            pst.setDouble(5, totalDays);
            pst.setString(6, reason);
            
            int affectedRows = pst.executeUpdate();
            
            if (affectedRows > 0) {
                showAlert(AlertType.INFORMATION, "Success", "Leave request submitted successfully for approval!");
                handleClose();
            } else {
                showAlert(AlertType.ERROR, "Submission Failed", "Request submission affected 0 rows.");
            }
            
        } catch (SQLException e) {
            showAlert(AlertType.ERROR, "Submission Failed", "Database error during request submission: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private boolean validateInput() {
        if (cbLeaveType.getValue() == null || dpStartDate.getValue() == null || 
            dpEndDate.getValue() == null || txtReason.getText().trim().isEmpty()) {
            showAlert(AlertType.WARNING, "Missing Data", "Please fill in all fields.");
            return false;
        }
        if (dpEndDate.getValue().isBefore(dpStartDate.getValue())) {
            showAlert(AlertType.WARNING, "Date Error", "End date cannot be before start date.");
            return false;
        }
        return true;
    }

    @FXML
    private void handleClose() {
        Stage stage = (Stage) txtReason.getScene().getWindow();
        stage.close();
    }
    
    private void showAlert(AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private Connection getDBConnection() throws SQLException {
        return DBConnection.getConnection();
    }
}